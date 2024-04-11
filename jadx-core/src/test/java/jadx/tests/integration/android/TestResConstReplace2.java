package jadx.tests.integration.android;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestResConstReplace2 extends IntegrationTest {

	public static class TestCls {
		public int test(int i) {
			switch (i) {
				case 0x0101013f: // android.R.attr.minWidth
					return 1;
				case 0x01010140: // android.R.attr.minHeight
					return 2;
				default:
					return 0;
			}

		}
	}

	@Test
	public void test() {
		disableCompilation();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("import android.R;")
				.containsOne("case R.attr.minWidth:");
	}
}
