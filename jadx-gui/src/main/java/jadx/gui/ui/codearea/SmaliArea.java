package jadx.gui.ui.codearea;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.EditorKit;
import javax.swing.text.JTextComponent;

import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.*;

import jadx.gui.device.debugger.BreakpointManager;
import jadx.gui.device.debugger.DbgUtils;
import jadx.gui.settings.JadxSettings;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.TextNode;
import jadx.gui.ui.ContentPanel;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

public final class SmaliArea extends AbstractCodeArea {
	private static final long serialVersionUID = 1334485631870306494L;

	private static final Icon ICON_BREAKPOINT = UiUtils.openIcon("breakpoint");
	private static final Icon ICON_BREAKPOINT_DISABLED = UiUtils.openIcon("breakpoint_disabled");
	private static final Color BREAKPOINT_LINE_COLOR = Color.decode("#FF986E");
	private static final Color DEBUG_LINE_COLOR = Color.decode("#80B4FF");

	private final JNode textNode;

	private SmaliV2Style smaliV2Style;
	private boolean curVersion = false;
	private final JCheckBoxMenuItem cbUseSmaliV2;

	private Gutter gutter; // for displaying breakpoint icon.
	private Object runningHighlightTag = null; // running line
	private Map<Integer, BreakpointLine> bpMap = Collections.emptyMap();

	SmaliArea(ContentPanel contentPanel) {
		super(contentPanel);
		this.textNode = new TextNode(node.getName());

		cbUseSmaliV2 = new JCheckBoxMenuItem(NLS.str("popup.bytecode_col"), shouldUseSmaliPrinterV2());
		cbUseSmaliV2.setAction(new AbstractAction(NLS.str("popup.bytecode_col")) {
			private static final long serialVersionUID = -1111111202103170737L;

			@Override
			public void actionPerformed(ActionEvent e) {

				boolean usingV2 = shouldUseSmaliPrinterV2();
				JadxSettings settings = getContentPanel().getTabbedPane().getMainWindow().getSettings();
				settings.setSmaliAreaShowBytecode(!usingV2);
				contentPanel.getTabbedPane().getOpenTabs().values().forEach(v -> {
					if (v instanceof ClassCodeContentPanel) {
						((ClassCodeContentPanel) v).getSmaliCodeArea().refresh();
					}
				});
				settings.sync();
			}
		});
		getPopupMenu().add(cbUseSmaliV2);
		if (shouldUseSmaliPrinterV2()) {
			loadV2Style();
		}
		regBreakpointRelevant();
	}

	private void regBreakpointRelevant() {
		KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0);
		UiUtils.addKeyBinding(this, key, "set break point", new AbstractAction() {
			private static final long serialVersionUID = -1111111202103170738L;

			@Override
			public void actionPerformed(ActionEvent e) {
				setBreakpoint(getCaretPosition());
			}
		});
		BreakpointManager.addListener((JClass) node, this::setBreakpointDisabled);
	}

	public void highLightDebuggingLineByPos(int pos) {
		if (runningHighlightTag != null) {
			removeLineHighlight(runningHighlightTag);
		}
		try {
			int line = getLineOfOffset(pos);
			runningHighlightTag = addLineHighlight(line, DEBUG_LINE_COLOR);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	private void loadBreakpoints() {
		List<Integer> posList = BreakpointManager.getPositions((JClass) node);
		for (Integer integer : posList) {
			setBreakpoint(integer);
		}
	}

	private void setBreakpoint(int pos) {
		if (!shouldUseSmaliPrinterV2()) {
			return;
		}
		int line;
		try {
			line = getLineOfOffset(pos);
		} catch (BadLocationException badLocationException) {
			badLocationException.printStackTrace();
			return;
		}
		BreakpointLine bpLine = bpMap.remove(line);
		if (bpLine == null) {
			bpLine = new BreakpointLine(line);
			bpLine.setDisabled(false);
			if (bpMap == Collections.EMPTY_MAP) {
				bpMap = new HashMap<>();
			}
			bpMap.put(line, bpLine);
			if (!BreakpointManager.set((JClass) node, line)) {
				bpLine.setDisabled(true);
			}
		} else {
			BreakpointManager.remove((JClass) node, line);
			bpLine.remove();
		}
	}

	private void setBreakpointDisabled(int pos) {
		try {
			int line = this.getLineOfOffset(pos);
			bpMap.computeIfAbsent(line, k -> new BreakpointLine(line)).setDisabled(true);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Font getFont() {
		if (smaliV2Style != null && shouldUseSmaliPrinterV2()) {
			return smaliV2Style.getFont();
		}
		return super.getFont();
	}

	@Override
	public Font getFontForTokenType(int type) {
		if (shouldUseSmaliPrinterV2()) {
			return smaliV2Style.getFont();
		}
		return super.getFontForTokenType(type);
	}

	private boolean shouldUseSmaliPrinterV2() {
		return getContentPanel().getTabbedPane().getMainWindow().getSettings().getSmaliAreaShowBytecode();
	}

	private void loadV2Style() {
		if (smaliV2Style == null) {
			smaliV2Style = new SmaliV2Style(this);
			addPropertyChangeListener(SYNTAX_SCHEME_PROPERTY, evt -> {
				if (smaliV2Style.refreshTheme() && shouldUseSmaliPrinterV2()) {
					setSyntaxScheme(smaliV2Style);
				}
			});
		}
		setSyntaxScheme(smaliV2Style);
	}

	@Override
	public void load() {
		boolean useSmaliV2 = shouldUseSmaliPrinterV2();
		if (useSmaliV2 != cbUseSmaliV2.getState()) {
			cbUseSmaliV2.setState(useSmaliV2);
		}
		if (getText().isEmpty() || curVersion != useSmaliV2) {
			curVersion = useSmaliV2;
			if (!useSmaliV2) {
				if (getSyntaxScheme() == smaliV2Style) {
					Theme theme = getContentPanel().getTabbedPane().getMainWindow().getEditorTheme();
					setSyntaxScheme(theme.scheme);
				}
				setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
				setText(node.getSmali());
			} else {
				loadV2Style();
				setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_6502);
				setText(DbgUtils.getSmaliCode(((JClass) node).getCls().getClassNode()));
			}

			if (gutter == null) {
				gutter = RSyntaxUtilities.getGutter(SmaliArea.this);
				gutter.setBookmarkIcon(ICON_BREAKPOINT);
				gutter.setBookmarkingEnabled(true);
				gutter.setActiveLineRangeColor(Color.RED);
				gutter.setIconRowHeaderInheritsGutterBackground(true);
				Font baseFont = this.getFont();
				gutter.setLineNumberFont(baseFont.deriveFont(baseFont.getSize2D() - 1.0f));
			}
			if (useSmaliV2) {
				loadBreakpoints();
			}
			setCaretPosition(0);
		}
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

	private static class SmaliV2Style extends SyntaxScheme {

		SmaliArea smaliArea;
		Theme curTheme;

		public SmaliV2Style(SmaliArea smaliArea) {
			super(true);
			this.smaliArea = smaliArea;
			curTheme = smaliArea.getContentPanel().getTabbedPane().getMainWindow().getEditorTheme();
			updateTheme();
		}

		public Font getFont() {
			return smaliArea.getContentPanel().getTabbedPane().getMainWindow().getSettings().getSmaliFont();
		}

		public boolean refreshTheme() {
			Theme theme = smaliArea.getContentPanel().getTabbedPane().getMainWindow().getEditorTheme();
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
						e.printStackTrace();
					}
				}
			} else {
				if (this.disabled) {
					gutter.removeTrackingIcon(this.iconInfo);
					try {
						iconInfo = gutter.addLineTrackingIcon(line, ICON_BREAKPOINT);
						highlightTag = addLineHighlight(line, BREAKPOINT_LINE_COLOR);
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
				}
			}
			this.disabled = disabled;
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
									SmaliArea.this.setBreakpoint(offs);
								}
							}
						};
					}
				};
			}
		};
	}
}
