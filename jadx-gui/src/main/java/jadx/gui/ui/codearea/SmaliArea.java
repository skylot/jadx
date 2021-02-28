package jadx.gui.ui.codearea;

import java.awt.*;
import java.awt.event.ActionEvent;

import javax.swing.*;

import org.fife.ui.rsyntaxtextarea.*;

import jadx.gui.settings.JadxSettings;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.TextNode;
import jadx.gui.ui.ContentPanel;
import jadx.gui.utils.NLS;

public final class SmaliArea extends AbstractCodeArea {
	private static final long serialVersionUID = 1334485631870306494L;

	private final JNode textNode;

	private SmaliV2Style smaliV2Style;
	private boolean curVersion = false;
	private final JCheckBoxMenuItem cbUseSmaliV2;

	SmaliArea(ContentPanel contentPanel) {
		super(contentPanel);
		this.textNode = new TextNode(node.getName());

		cbUseSmaliV2 = new JCheckBoxMenuItem(NLS.str("popup.bytecode_col"), shouldUseSmaliPrinterV2());
		cbUseSmaliV2.setAction(new AbstractAction(NLS.str("popup.bytecode_col")) {
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
				setText(((JClass) node).getSmaliV2());
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
}
