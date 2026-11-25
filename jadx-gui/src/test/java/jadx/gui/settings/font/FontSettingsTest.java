package jadx.gui.settings.font;

import java.awt.Font;
import java.awt.FontMetrics;

import javax.swing.JLabel;

import org.junit.jupiter.api.Test;

import com.formdev.flatlaf.FlatLightLaf;

import jadx.commons.app.JadxSystemInfo;
import jadx.gui.settings.JadxSettingsData;
import jadx.gui.utils.FontUtils;
import jadx.gui.utils.NLS;

import static org.assertj.core.api.Assertions.assertThat;

class FontSettingsTest {

	@Test
	void testDefaultFontsSupportCjkAndMonospacedCodeOnMac() {
		NLS.setLocale(NLS.defaultLocale());
		FlatLightLaf.setup();

		JadxSettingsData data = new JadxSettingsData();
		FontSettings fontSettings = new FontSettings();
		fontSettings.bindData(data);
		fontSettings.updateDefaultFont();

		// Default fonts should stay empty in persisted settings data
		assertThat(data.getUiFontStr()).isEmpty();
		assertThat(data.getCodeFontStr()).isEmpty();
		assertThat(data.getSmaliFontStr()).isEmpty();

		Font uiFont = fontSettings.getUiFontAdapter().getEffectiveFont();
		Font codeFont = fontSettings.getCodeFontAdapter().getEffectiveFont();
		Font smaliFont = fontSettings.getSmaliFontAdapter().getEffectiveFont();

		assertThat(uiFont).isNotNull();
		assertThat(codeFont).isNotNull();
		assertThat(smaliFont).isNotNull();

		if (JadxSystemInfo.IS_MAC) {
			String cjkSample = "中文日本語한국어";
			assertThat(FontUtils.canStringBeDisplayed(cjkSample, uiFont)).isTrue();
			assertThat(FontUtils.canStringBeDisplayed(cjkSample, codeFont)).isTrue();
			assertThat(FontUtils.canStringBeDisplayed(cjkSample, smaliFont)).isTrue();

			JLabel label = new JLabel();
			FontMetrics codeFm = label.getFontMetrics(codeFont);
			FontMetrics smaliFm = label.getFontMetrics(smaliFont);
			assertThat(codeFm.charWidth('i')).isEqualTo(codeFm.charWidth('W'));
			assertThat(smaliFm.charWidth('i')).isEqualTo(smaliFm.charWidth('W'));
		}
	}

	@Test
	void testUpdateDefaultFontPreservesCustomUserFont() {
		NLS.setLocale(NLS.defaultLocale());
		FlatLightLaf.setup();

		JadxSettingsData data = new JadxSettingsData();
		data.setCodeFontStr("Monospaced/plain/16");

		FontSettings fontSettings = new FontSettings();
		fontSettings.bindData(data);
		fontSettings.updateDefaultFont();

		Font codeFont = fontSettings.getCodeFontAdapter().getEffectiveFont();
		assertThat(codeFont.getSize()).isEqualTo(16);
		assertThat(data.getCodeFontStr()).isEqualTo("Monospaced/plain/16");
	}
}
