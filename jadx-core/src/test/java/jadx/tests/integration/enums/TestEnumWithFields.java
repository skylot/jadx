package jadx.tests.integration.enums;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

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
			assertThat(SearchTimeout.DISABLED.sec).isEqualTo(0);
			assertThat(SearchTimeout.DEFAULT.sec).isEqualTo(0);
			assertThat(SearchTimeout.TWO_SECONDS.sec).isEqualTo(2);
			assertThat(SearchTimeout.MAX.sec).isEqualTo(5);
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
