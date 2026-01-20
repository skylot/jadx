package jadx.gui.settings.font;

import java.awt.Font;

import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;
import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont;
import com.formdev.flatlaf.util.FontUtils;

import jadx.gui.settings.JadxSettingsData;
import jadx.gui.utils.UiUtils;

/**
 * Handle all font related settings
 */
public class FontSettings {

	static {
		FlatInterFont.install();
		FlatJetBrainsMonoFont.install();
		FlatLaf.setPreferredMonospacedFontFamily(FlatJetBrainsMonoFont.FAMILY);
	}

	private final FontAdapter uiFontAdapter;
	private final FontAdapter codeFontAdapter;
	private final FontAdapter smaliFontAdapter;

	private float uiZoom;
	private boolean applyUiZoomToFonts;

	public FontSettings() {
		int defFontSize = 13;
		Font defUiFont = FontUtils.getCompositeFont(FlatInterFont.FAMILY, Font.PLAIN, defFontSize);
		Font defCodeFont = FontUtils.getCompositeFont(FlatJetBrainsMonoFont.FAMILY, Font.PLAIN, defFontSize);
		uiFontAdapter = new FontAdapter(defUiFont);
		codeFontAdapter = new FontAdapter(defCodeFont);
		smaliFontAdapter = new FontAdapter(defCodeFont);
	}

	public void bindData(JadxSettingsData data) {
		uiFontAdapter.bindData(data.getUiFontStr(), data::setUiFontStr);
		codeFontAdapter.bindData(data.getCodeFontStr(), data::setCodeFontStr);
		smaliFontAdapter.bindData(data.getSmaliFontStr(), data::setSmaliFontStr);
		applyUiZoom(data.getUiZoom(), data.isApplyUiZoomToFonts());
	}

	/**
	 * Fetch and apply default font settings after FlatLaf init.
	 */
	public void updateDefaultFont() {
		Font defaultFont = UIManager.getFont("defaultFont");
		if (defaultFont != null) {
			uiFontAdapter.setDefaultFont(defaultFont);
		}
	}

	public synchronized void applyUiZoom(float newUiZoom, boolean newApplyUiZoomToFonts) {
		if (UiUtils.nearlyEqual(uiZoom, newUiZoom) && applyUiZoomToFonts == newApplyUiZoomToFonts) {
			return;
		}
		uiZoom = newUiZoom;
		applyUiZoomToFonts = newApplyUiZoomToFonts;

		float effectiveFontZoom = newApplyUiZoomToFonts ? newUiZoom : 1.0f;
		uiFontAdapter.setUiZoom(effectiveFontZoom);
		codeFontAdapter.setUiZoom(effectiveFontZoom);
		smaliFontAdapter.setUiZoom(effectiveFontZoom);
	}

	public FontAdapter getUiFontAdapter() {
		return uiFontAdapter;
	}

	public FontAdapter getCodeFontAdapter() {
		return codeFontAdapter;
	}

	public FontAdapter getSmaliFontAdapter() {
		return smaliFontAdapter;
	}
}
