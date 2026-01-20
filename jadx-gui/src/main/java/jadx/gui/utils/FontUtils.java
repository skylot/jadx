package jadx.gui.utils;

import java.awt.Font;
import java.io.InputStream;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.exceptions.JadxRuntimeException;

public class FontUtils {
	private static final Logger LOG = LoggerFactory.getLogger(FontUtils.class);

	public static Font loadByStr(String fontDesc) {
		String[] parts = fontDesc.split("/");
		if (parts.length != 3) {
			throw new JadxRuntimeException("Unsupported font description format: " + fontDesc);
		}
		String family = parts[0];
		int style = parseFontStyle(parts[1]);
		int size = Integer.parseInt(parts[2]);

		Font font = FontUtils.getCompositeFont(family, style, size);
		if (font == null) {
			throw new JadxRuntimeException("Font not found: " + fontDesc);
		}
		return font;
	}

	public static String convertToStr(@Nullable Font font) {
		if (font == null) {
			return "";
		}
		if (font.getSize() < 1) {
			throw new JadxRuntimeException("Bad font size: " + font.getSize());
		}
		return font.getFamily()
				+ '/' + convertFontStyleToString(font.getStyle())
				+ '/' + font.getSize();
	}

	public static String convertFontStyleToString(int style) {
		if (style == 0) {
			return "plain";
		}
		StringBuilder sb = new StringBuilder();
		if ((style & Font.BOLD) != 0) {
			sb.append("bold");
		}
		if ((style & Font.ITALIC) != 0) {
			sb.append(" italic");
		}
		return sb.toString().trim();
	}

	private static int parseFontStyle(String str) {
		int style = 0;
		if (str.contains("bold")) {
			style |= Font.BOLD;
		}
		if (str.contains("italic")) {
			style |= Font.ITALIC;
		}
		return style;
	}

	@Nullable
	public static Font openFontTTF(String name) {
		String fontPath = "/fonts/" + name + ".ttf";
		try (InputStream is = UiUtils.class.getResourceAsStream(fontPath)) {
			Font font = Font.createFont(Font.TRUETYPE_FONT, is);
			return font.deriveFont(12f);
		} catch (Exception e) {
			LOG.error("Failed load font by path: {}", fontPath, e);
			return null;
		}
	}

	public static boolean canStringBeDisplayed(String str, Font font) {
		if (str == null || str.isEmpty()) {
			return true;
		}
		int offset = 0;
		while (offset < str.length()) {
			int codePoint = str.codePointAt(offset);
			if (!font.canDisplay(codePoint)) {
				return false;
			}
			offset += Character.charCount(codePoint);
		}
		return true;
	}

	/**
	 * https://github.com/JFormDesigner/FlatLaf/issues/923
	 * When changing fonts, use font.deriveFont() or FontUtils.getCompositeFont
	 * instead of new Font(), otherwise FlatLaf's CJKs support will be lost
	 */
	public static Font getCompositeFont(String family, int style, int size) {
		return com.formdev.flatlaf.util.FontUtils.getCompositeFont(family, style, size);
	}

	private FontUtils() {
	}
}
