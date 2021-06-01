package jadx.tests.integration.enums;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestEnumWithFields extends SmaliTest {

	public static class TestCls {

		public enum SearchTimeout {
			DISABLED(0), TWO_SECONDS(2), FIVE_SECONDS(5);

			public static final SearchTimeout DEFAULT = DISABLED;
			public static final SearchTimeout MAX = FIVE_SECONDS;

			public final int sec;

			SearchTimeout(int val) {
				this.sec = val;
			}
		}

		public void check() {
			assertEquals(0, SearchTimeout.DISABLED.sec);
			assertEquals(0, SearchTimeout.DEFAULT.sec);
			assertEquals(2, SearchTimeout.TWO_SECONDS.sec);
			assertEquals(5, SearchTimeout.MAX.sec);
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code();
	}

	@Test
	public void test2() {
		assertThat(getClassNodeFromSmali())
				.code();
	}
}
