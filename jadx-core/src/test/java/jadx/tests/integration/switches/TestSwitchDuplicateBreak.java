package jadx.tests.integration.switches;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSwitchDuplicateBreak extends IntegrationTest {

	public static class TestCls {
		public static String parse(String raw) {
			String currency;
			switch (raw.charAt(0)) {
				case '0':
					currency = "£";
					break;
				case '5':
					currency = "$";
					break;
				case '9':
					if ("90000".equals(raw)) {
						return null;
					}
					if ("99991".equals(raw)) {
						return "0.00";
					}
					if ("99990".equals(raw)) {
						return "Used";
					}
					currency = "";
					break;
				default:
					currency = "";
					break;
			}
			int rawAmount = Integer.parseInt(raw.substring(1));
			String unitsString = String.valueOf(rawAmount / 100);
			int hundredths = rawAmount % 100;
			String hundredthsString = hundredths < 10 ? "0" + hundredths : String.valueOf(hundredths);
			return currency + unitsString + '.' + hundredthsString;
		}

		public void check() {
			assertThat(parse("01234")).isEqualTo("£12.34");
			assertThat(parse("50500")).isEqualTo("$5.00");
			assertThat(parse("90000")).isNull();
			assertThat(parse("99991")).isEqualTo("0.00");
			assertThat(parse("99990")).isEqualTo("Used");
			assertThat(parse("91234")).isEqualTo("12.34");
			assertThat(parse("11234")).isEqualTo("12.34");
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.D8_J11, TestProfile.JAVA11 })
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("break;\n        break;")
				.containsOne("public static String parse(");
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.D8_J11, TestProfile.JAVA11 })
	public void testNoDebug() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("break;\n        break;")
				.containsOne("public static String parse(");
	}
}
