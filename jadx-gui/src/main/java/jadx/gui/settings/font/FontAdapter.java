package jadx.gui.settings.font;

import java.awt.Font;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.Utils;
import jadx.gui.utils.FontUtils;
import jadx.gui.utils.UiUtils;

/**
 * Common handler for font updates and sync with settings data.
 */
public class FontAdapter {
	private static final Logger LOG = LoggerFactory.getLogger(FontAdapter.class);

	private Font defaultFont;
	private Font font;
	private Font effectiveFont;
	private Consumer<String> fontSetter;
	private float uiZoom;

	public FontAdapter(Font defaultFont) {
		Objects.requireNonNull(defaultFont);
		this.defaultFont = defaultFont;
		this.font = defaultFont;
	}

	/**
	 * Load current font from data, and save font setter to future sync
	 */
	public void bindData(String fontStr, Consumer<String> fontStrSetter) {
		font = loadFromStr(fontStr);
		fontSetter = fontStrSetter;
	}

	public void setDefaultFont(Font newDefaultFont) {
		Objects.requireNonNull(newDefaultFont);
		Font prevDefaultFont = defaultFont;
		defaultFont = newDefaultFont;
		if (font == prevDefaultFont) {
			// font was set to default => update it also
			setFont(newDefaultFont);
		}
	}

	public Font getFont() {
		return font;
	}

	public Font getEffectiveFont() {
		return effectiveFont;
	}

	public void setFont(@Nullable Font newFont) {
		font = Utils.getOrElse(newFont, defaultFont);
		fontSetter.accept(getFontStr());
		applyFontZoom();
	}

	public void setUiZoom(float uiZoom) {
		this.uiZoom = uiZoom;
		applyFontZoom();
	}

	private Font loadFromStr(String fontStr) {
		if (fontStr != null && !fontStr.isEmpty()) {
			try {
				return FontUtils.loadByStr(fontStr);
			} catch (Exception e) {
				LOG.warn("Failed to load font: {}, reset to default", fontStr, e);
			}
		}
		return defaultFont;
	}

	private String getFontStr() {
		if (font == defaultFont) {
			return "";
		}
		return FontUtils.convertToStr(font);
	}

	private void applyFontZoom() {
		if (UiUtils.nearlyEqual(uiZoom, 1.0f)) {
			effectiveFont = font;
		} else {
			effectiveFont = font.deriveFont(font.getSize2D() * uiZoom);
		}
	}
}
