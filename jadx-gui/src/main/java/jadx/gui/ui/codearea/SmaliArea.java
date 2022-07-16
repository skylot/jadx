package jadx.gui.ui.codearea;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.EditorKit;
import javax.swing.text.JTextComponent;

import org.fife.ui.rsyntaxtextarea.FoldingAwareIconRowHeader;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaUI;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.GutterIconInfo;
import org.fife.ui.rtextarea.IconRowHeader;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextAreaUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeInfo;
import jadx.gui.device.debugger.BreakpointManager;
import jadx.gui.device.debugger.DbgUtils;
import jadx.gui.settings.JadxSettings;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.TextNode;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

public final class SmaliArea extends AbstractCodeArea {
	private static final Logger LOG = LoggerFactory.getLogger(SmaliArea.class);

	private static final long serialVersionUID = 1334485631870306494L;

	private static final Icon ICON_BREAKPOINT = UiUtils.openSvgIcon("debugger/db_set_breakpoint");
	private static final Icon ICON_BREAKPOINT_DISABLED = UiUtils.openSvgIcon("debugger/db_disabled_breakpoint");
	private static final Color BREAKPOINT_LINE_COLOR = Color.decode("#ad103c");
	private static final Color DEBUG_LINE_COLOR = Color.decode("#9c1138");

	private final JNode textNode;
	private final JCheckBoxMenuItem cbUseSmaliV2;
	private boolean curVersion = false;
	private SmaliModel model;

	SmaliArea(ContentPanel contentPanel, JClass node) {
		super(contentPanel, node);
		this.textNode = new TextNode(node.getName());

		cbUseSmaliV2 = new JCheckBoxMenuItem(NLS.str("popup.bytecode_col"),
				shouldUseSmaliPrinterV2());
		cbUseSmaliV2.setAction(new AbstractAction(NLS.str("popup.bytecode_col")) {
			private static final long serialVersionUID = -1111111202103170737L;

			@Override
			public void actionPerformed(ActionEvent e) {
				JadxSettings settings = getContentPanel().getTabbedPane().getMainWindow().getSettings();
				settings.setSmaliAreaShowBytecode(!settings.getSmaliAreaShowBytecode());
				contentPanel.getTabbedPane().getOpenTabs().values().forEach(v -> {
					if (v instanceof ClassCodeContentPanel) {
						switchModel();
						((ClassCodeContentPanel) v).getSmaliCodeArea().refresh();
					}
				});
				settings.sync();
			}
		});
		getPopupMenu().add(cbUseSmaliV2);
		switchModel();
	}

	@Override
	public void load() {
		if (getText().isEmpty() || curVersion != shouldUseSmaliPrinterV2()) {
			curVersion = shouldUseSmaliPrinterV2();
			model.load();
			setCaretPosition(0);
			setLoaded();
		}
	}

	@Override
	public ICodeInfo getCodeInfo() {
		return ICodeInfo.EMPTY;
	}

	@Override
	public void refresh() {
		load();
	}

	@Override
	public JNode getNode() {
		// this area contains only smali without other node attributes
		return textNode;
	}

	public JClass getJClass() {
		return ((JClass) node);
	}

	private void switchModel() {
		if (model != null) {
			model.unload();
		}
		model = shouldUseSmaliPrinterV2() ? new DebugModel() : new NormalModel();
	}

	public void scrollToDebugPos(int pos) {
		getContentPanel().getTabbedPane().getMainWindow()
				.getSettings().setSmaliAreaShowBytecode(true); // don't sync when it's set programmatically.
		cbUseSmaliV2.setState(shouldUseSmaliPrinterV2());
		if (!(model instanceof DebugModel)) {
			switchModel();
			refresh();
		}
		model.togglePosHighlight(pos);
	}

	@Override
	public Font getFont() {
		if (model == null || isDisposed()) {
			return super.getFont();
		}
		return model.getFont();
	}

	@Override
	public Font getFontForTokenType(int type) {
		return getFont();
	}

	private boolean shouldUseSmaliPrinterV2() {
		return getContentPanel().getTabbedPane().getMainWindow().getSettings().getSmaliAreaShowBytecode();
	}

	private abstract class SmaliModel {
		abstract void load();

		abstract void unload();

		Font getFont() {
			return SmaliArea.super.getFont();
		}

		Font getFontForTokenType(int type) {
			return SmaliArea.super.getFontForTokenType(type);
		}

		void setBreakpoint(int off) {
		}

		void togglePosHighlight(int pos) {
		}
	}

	private class NormalModel extends SmaliModel {

		public NormalModel() {
			Theme theme = getContentPanel().getTabbedPane().getMainWindow().getEditorTheme();
			setSyntaxScheme(theme.scheme);
			setSyntaxEditingStyle(SYNTAX_STYLE_SMALI);
		}

		@Override
		public void load() {
			setText(getJClass().getSmali());
		}

		@Override
		public void unload() {

		}
	}

	private class DebugModel extends SmaliModel {
		private KeyStroke bpShortcut;
		private final String keyID = "set a break point";
		private Gutter gutter;
		private Object runningHighlightTag = null; // running line
		private final SmaliV2Style smaliV2Style = new SmaliV2Style(SmaliArea.this);
		private final Map<Integer, BreakpointLine> bpMap = new HashMap<>();
		private final PropertyChangeListener listener = evt -> {
			if (smaliV2Style.refreshTheme()) {
				setSyntaxScheme(smaliV2Style);
			}
		};

		public DebugModel() {
			loadV2Style();
			setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_6502);
			addPropertyChangeListener(SYNTAX_SCHEME_PROPERTY, listener);
			regBreakpointEvents();
		}

		@Override
		public void load() {
			if (gutter == null) {
				gutter = RSyntaxUtilities.getGutter(SmaliArea.this);
				gutter.setBookmarkingEnabled(true);
				gutter.setIconRowHeaderInheritsGutterBackground(true);
				Font baseFont = SmaliArea.super.getFont();
				gutter.setLineNumberFont(baseFont.deriveFont(baseFont.getSize2D() - 1.0f));
			}
			setText(DbgUtils.getSmaliCode(((JClass) node).getCls().getClassNode()));
			loadV2Style();
			loadBreakpoints();
		}

		@Override
		public void unload() {
			removePropertyChangeListener(listener);
			removeLineHighlight(runningHighlightTag);
			UiUtils.removeKeyBinding(SmaliArea.this, bpShortcut, keyID);
			BreakpointManager.removeListener((JClass) node);
			bpMap.forEach((k, v) -> {
				v.remove();
			});
		}

		@Override
		public Font getFont() {
			return smaliV2Style.getFont();
		}

		@Override
		public Font getFontForTokenType(int type) {
			return smaliV2Style.getFont();
		}

		private void loadV2Style() {
			setSyntaxScheme(smaliV2Style);
		}

		private void regBreakpointEvents() {
			bpShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0);
			UiUtils.addKeyBinding(SmaliArea.this, bpShortcut, "set break point", new AbstractAction() {
				private static final long serialVersionUID = -1111111202103170738L;

				@Override
				public void actionPerformed(ActionEvent e) {
					setBreakpoint(getCaretPosition());
				}
			});
			BreakpointManager.addListener((JClass) node, this::setBreakpointDisabled);
		}

		private void loadBreakpoints() {
			List<Integer> posList = BreakpointManager.getPositions((JClass) node);
			for (Integer integer : posList) {
				setBreakpoint(integer);
			}
		}

		@Override
		public void setBreakpoint(int pos) {
			int line;
			try {
				line = getLineOfOffset(pos);
			} catch (BadLocationException e) {
				LOG.error("Failed to get line by offset: {}", pos, e);
				return;
			}
			BreakpointLine bpLine = bpMap.remove(line);
			if (bpLine == null) {
				bpLine = new BreakpointLine(line);
				bpLine.setDisabled(false);
				bpMap.put(line, bpLine);
				if (!BreakpointManager.set((JClass) node, line)) {
					bpLine.setDisabled(true);
				}
			} else {
				BreakpointManager.remove((JClass) node, line);
				bpLine.remove();
			}
		}

		@Override
		public void togglePosHighlight(int pos) {
			if (runningHighlightTag != null) {
				removeLineHighlight(runningHighlightTag);
			}
			try {
				int line = getLineOfOffset(pos);
				runningHighlightTag = addLineHighlight(line, DEBUG_LINE_COLOR);
			} catch (BadLocationException e) {
				LOG.error("Failed to get line by offset: {}", pos, e);
			}
		}

		private void setBreakpointDisabled(int pos) {
			try {
				int line = getLineOfOffset(pos);
				bpMap.computeIfAbsent(line, k -> new BreakpointLine(line)).setDisabled(true);
			} catch (BadLocationException e) {
				LOG.error("Failed to get line by offset: {}", pos, e);
			}
		}

		private class SmaliV2Style extends SyntaxScheme {

			Theme curTheme;

			public SmaliV2Style(SmaliArea smaliArea) {
				super(true);
				curTheme = smaliArea.getContentPanel().getTabbedPane().getMainWindow().getEditorTheme();
				updateTheme();
			}

			public Font getFont() {
				return getContentPanel().getTabbedPane().getMainWindow().getSettings().getSmaliFont();
			}

			public boolean refreshTheme() {
				Theme theme = getContentPanel().getTabbedPane().getMainWindow().getEditorTheme();
				boolean refresh = theme != curTheme;
				if (refresh) {
					curTheme = theme;
					updateTheme();
				}
				return refresh;
			}

			private void updateTheme() {
				Style[] mainStyles = curTheme.scheme.getStyles();
				Style[] styles = new Style[mainStyles.length];
				for (int i = 0; i < mainStyles.length; i++) {
					Style mainStyle = mainStyles[i];
					if (mainStyle == null) {
						styles[i] = new Style();
					} else {
						// font will be hijacked by getFont & getFontForTokenType,
						// so it doesn't need to be set here.
						styles[i] = new Style(mainStyle.foreground, mainStyle.background, null);
					}
				}
				setStyles(styles);
			}

			@Override
			public void restoreDefaults(Font baseFont) {
				restoreDefaults(baseFont, true);
			}

			@Override
			public void restoreDefaults(Font baseFont, boolean fontStyles) {
				// Note: it's a hook for continue using the editor theme, better don't remove it.
			}
		}

		private class BreakpointLine {
			Object highlightTag;
			GutterIconInfo iconInfo;
			boolean disabled;
			final int line;

			BreakpointLine(int line) {
				this.line = line;
				this.disabled = true;
			}

			void remove() {
				gutter.removeTrackingIcon(iconInfo);
				if (!this.disabled) {
					removeLineHighlight(highlightTag);
				}
			}

			void setDisabled(boolean disabled) {
				if (disabled) {
					if (!this.disabled) {
						gutter.removeTrackingIcon(iconInfo);
						removeLineHighlight(highlightTag);
						try {
							iconInfo = gutter.addLineTrackingIcon(line, ICON_BREAKPOINT_DISABLED);
						} catch (BadLocationException e) {
							LOG.error("Failed to add line tracking icon", e);
						}
					}
				} else {
					if (this.disabled) {
						gutter.removeTrackingIcon(this.iconInfo);
						try {
							iconInfo = gutter.addLineTrackingIcon(line, ICON_BREAKPOINT);
							highlightTag = addLineHighlight(line, BREAKPOINT_LINE_COLOR);
						} catch (BadLocationException e) {
							LOG.error("Failed to remove line tracking icon", e);
						}
					}
				}
				this.disabled = disabled;
			}
		}
	}

	@Override
	protected RTextAreaUI createRTextAreaUI() {
		// IconRowHeader won't fire an event when people click on it for adding/removing icons,
		// so our poor breakpoints won't be set if we don't hijack IconRowHeader.
		return new RSyntaxTextAreaUI(this) {
			@Override
			public EditorKit getEditorKit(JTextComponent tc) {
				return new RSyntaxTextAreaEditorKit() {
					private static final long serialVersionUID = -1111111202103170740L;

					@Override
					public IconRowHeader createIconRowHeader(RTextArea textArea) {
						return new FoldingAwareIconRowHeader((RSyntaxTextArea) textArea) {
							private static final long serialVersionUID = -1111111202103170739L;

							@Override
							public void mousePressed(MouseEvent e) {
								int offs = textArea.viewToModel(e.getPoint());
								if (offs > -1) {
									model.setBreakpoint(offs);
								}
							}
						};
					}
				};
			}
		};
	}
}
