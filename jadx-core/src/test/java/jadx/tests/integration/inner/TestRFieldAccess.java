package jadx.tests.integration.inner;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.countString;
import static org.junit.Assert.assertThat;

public class TestRFieldAccess extends IntegrationTest {

	public static class R {
		public static final class id {
			public static final int Button01 = 2131230730;
		}
	}

	public static class TestR {
		public int test() {
			return R.id.Button01;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestRFieldAccess.class);
		String code = cls.getCode().toString();
		assertThat(code, countString(2, "return R.id.Button01;"));
	}
}
