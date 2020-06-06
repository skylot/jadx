package jadx.tests.integration.android;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.countString;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings("TypeName")
public class TestRFieldAccess extends IntegrationTest {

	public static class R {
		public static final class id {
			public static final int BUTTON_01 = 2131230730;
		}
	}

	public static class TestR {
		public int test() {
			return R.id.BUTTON_01;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestRFieldAccess.class);
		String code = cls.getCode().toString();
		assertThat(code, countString(2, "return R.id.BUTTON_01;"));
	}
}
