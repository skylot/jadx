package jadx.tests.integration.android;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

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
		assertThat(getClassNode(TestRFieldAccess.class))
				.code()
				.countString(2, "return R.id.BUTTON_01;");
	}
}
