package jadx.gui.ui.panel;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.ByteArrayInputStream;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import jadx.api.ResourceFile;
import jadx.api.ResourcesLoader;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.xmlgen.ResContainer;
import jadx.gui.treemodel.JResource;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.ui.tab.TabbedPane;
import jadx.gui.utils.NLS;

public class FontPanel extends ContentPanel {
	private static final long serialVersionUID = 695370628262996993L;
	private static final String DEFAULT_PREVIEW_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ\n"
			+ "abcdefghijklmnopqrstuvwxyz\n"
			+ "1234567890!@#$%^&*()_-=+[]{}<,.>";

	public FontPanel(TabbedPane panel, JResource res) {
		super(panel, res);
		setLayout(new BorderLayout());
		RSyntaxTextArea textArea = AbstractCodeArea.getDefaultArea(panel.getMainWindow());
		add(textArea, BorderLayout.CENTER);
		try {
			Font selectedFont = loadFont(res);
			if (selectedFont.canDisplay(DEFAULT_PREVIEW_STRING.codePointAt(0))) {
				textArea.setFont(selectedFont);
				textArea.setText(DEFAULT_PREVIEW_STRING);
			} else {
				textArea.setText(NLS.str("message.unable_preview_font"));
			}
		} catch (Exception e) {
			textArea.setText("Font load error:\n" + Utils.getStackTrace(e));
		}
	}

	private Font loadFont(JResource res) {
		ResourceFile resFile = res.getResFile();
		ResContainer resContainer = resFile.loadContent();
		ResContainer.DataType dataType = resContainer.getDataType();
		if (dataType == ResContainer.DataType.DECODED_DATA) {
			try {
				return Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(resContainer.getDecodedData())).deriveFont(12f);
			} catch (Exception e) {
				throw new JadxRuntimeException("Failed to load font", e);
			}
		} else if (dataType == ResContainer.DataType.RES_LINK) {
			try {
				return ResourcesLoader.decodeStream(resFile, (size, is) -> {
					try {
						return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(12f);
					} catch (FontFormatException e) {
						throw new JadxRuntimeException("Failed to load font", e);
					}
				});
			} catch (Exception e) {
				throw new JadxRuntimeException("Failed to load font", e);
			}
		} else {
			throw new JadxRuntimeException("Unsupported resource font data type: " + resFile);
		}
	}

	@Override
	public void loadSettings() {
		// no op
	}
}
