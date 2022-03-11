package jadx.gui.utils;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;

import javax.swing.text.StyleContext;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.exceptions.JadxRuntimeException;

public class FontUtils {
	private static final Logger LOG = LoggerFactory.getLogger(FontUtils.class);

	public static final Font FONT_HACK = openFontTTF("Hack-Regular");

	public static void registerBundledFonts() {
		GraphicsEnvironment grEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
		if (FontUtils.FONT_HACK != null) {
			grEnv.registerFont(FontUtils.FONT_HACK);
		}
	}

	public static Font loadByStr(String fontDesc) {
		String[] parts = fontDesc.split("/");
		if (parts.length != 3) {
			throw new JadxRuntimeException("Unsupported font description format: " + fontDesc);
		}
		String name = parts[0];
		int style = parseFontStyle(parts[1]);
		int size = Integer.parseInt(parts[2]);

		StyleContext sc = StyleContext.getDefaultStyleContext();
		Font font = sc.getFont(name, style, size);
		if (font == null) {
			throw new JadxRuntimeException("Font not found: " + fontDesc);
		}
		return font;
	}

	public static String convertToStr(Font font) {
		if (font.getSize() < 1) {
			throw new JadxRuntimeException("Bad font size: " + font.getSize());
		}
		return font.getFontName()
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

	private FontUtils() {
	}
}
