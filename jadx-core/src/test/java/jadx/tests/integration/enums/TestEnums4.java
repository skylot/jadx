package jadx.tests.integration.enums;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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
			assertThat(ResType.CODE.getExts(), is(new String[] { ".dex", ".class" }));
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("CODE(\".dex\", \".class\"),"));
		assertThat(code, containsOne("ResType(String... extensions) {"));
		// assertThat(code, not(containsString("private ResType")));
	}
}
