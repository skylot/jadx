package jadx.tests.integration.variables;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Generated variable names should not depend on the default locale
 * (in Turkish locale 'I' is lowercased to dotless 'ı')
 */
public class TestVariablesTurkishLocale extends IntegrationTest {

	public static class TestCls {
		public int test(int[] arr) {
			int sum = 0;
			for (int a : arr) {
				sum += a;
			}
			return sum;
		}
	}

	private Locale defaultLocale;

	@BeforeEach
	public void setTurkishLocale() {
		defaultLocale = Locale.getDefault();
		Locale.setDefault(Locale.forLanguageTag("tr-TR"));
	}

	@AfterEach
	public void restoreLocale() {
		Locale.setDefault(defaultLocale);
	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.contains("public int test(int[] iArr) {")
				.doesNotContain("ı");
	}
}
