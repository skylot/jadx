package jadx.tests.integration.enums;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestEnums4 extends IntegrationTest {

	public static class TestCls {
		public enum ResType {
			CODE(".dex", ".class"),
			MANIFEST("AndroidManifest.xml"),
			XML(".xml"),
			ARSC(".arsc"),
			FONT(".ttf"),
			IMG(".png", ".gif", ".jpg"),
			LIB(".so"),
			UNKNOWN;

			private final String[] exts;

			ResType(String... extensions) {
				this.exts = extensions;
			}

			public String[] getExts() {
				return exts;
			}
		}

		public void check() {
			assertThat(ResType.CODE.getExts()).containsExactly(new String[] { ".dex", ".class" });
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("CODE(\".dex\", \".class\"),")
				.containsOne("ResType(String... extensions) {");
		// assertThat(code, not(containsString("private ResType")));
	}
}
