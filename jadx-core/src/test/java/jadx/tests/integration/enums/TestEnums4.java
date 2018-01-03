package jadx.tests.integration.enums;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

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

			private ResType(String... exts) {
				this.exts = exts;
			}

			public String[] getExts() {
				return exts;
			}
		}

		public void check() {
			assertThat(ResType.CODE.getExts(), is(new String[]{".dex", ".class"}));
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("CODE(\".dex\", \".class\"),"));
		assertThat(code, containsOne("ResType(String... exts) {"));
//		assertThat(code, not(containsString("private ResType")));
	}
}
