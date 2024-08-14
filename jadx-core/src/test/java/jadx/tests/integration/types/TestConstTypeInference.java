package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

public class TestConstTypeInference extends IntegrationTest {

	@SuppressWarnings({ "overrides", "EqualsHashCode" })
	public static class TestCls {
		private final int a;

		public TestCls() {
			this(0);
		}

		public TestCls(int a) {
			this.a = a;
		}

		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj != null) {
				if (getClass() == obj.getClass()) {
					TestCls other = (TestCls) obj;
					return this.a == other.a;
				}
			}
			return false;
		}

		public void check() {
			TestCls seven = new TestCls(7);
			assertThat(seven).isEqualTo(seven);
			assertThat(seven).isNotEqualTo(null);

			TestCls six = new TestCls(6);
			assertThat(six).isNotEqualTo(seven);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("obj == this")
				.containsOneOf("obj == null", "obj != null");
	}

	@Test
	public void test2() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}
}
