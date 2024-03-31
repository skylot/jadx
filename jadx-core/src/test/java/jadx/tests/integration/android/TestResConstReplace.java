package jadx.tests.integration.android;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestResConstReplace extends IntegrationTest {

	public static class TestCls {
		public int test() {
			return 0x0101013f; // android.R.attr.minWidth
		}
	}

	@Test
	public void test() {
		disableCompilation();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("import android.R;")
				.containsOne("return R.attr.minWidth;");
	}
}
