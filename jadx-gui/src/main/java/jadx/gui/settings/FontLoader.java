package jadx.gui.settings;

import java.awt.Font;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.Utils;
import jadx.gui.utils.FontUtils;

public class FontLoader {
	private static final Logger LOG = LoggerFactory.getLogger(FontLoader.class);

	private final Font defaultFont;

	private Font font;

	public FontLoader(Font defaultFont) {
		this.defaultFont = defaultFont;
		this.font = defaultFont;
	}

	public Font getFont() {
		return font;
	}

	public void setFont(@Nullable Font font) {
		this.font = Utils.getOrElse(font, defaultFont);
	}

	public void load(String fontStr) {
		if (fontStr == null || fontStr.isEmpty()) {
			font = defaultFont;
		} else {
			try {
				font = FontUtils.loadByStr(fontStr);
			} catch (Exception e) {
				LOG.warn("Failed to load font: {}, reset to default", fontStr, e);
				font = defaultFont;
			}
		}
	}

	public String getFontStr() {
		return FontUtils.convertToStr(font);
	}
}
