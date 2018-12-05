package jadx.tests.integration.types;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.Matchers.anyOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

public class TestConstTypeInference extends IntegrationTest {

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
			assertEquals(seven, seven);
			assertNotEquals(seven, null);

			TestCls six = new TestCls(6);
			assertNotEquals(seven, six);
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("obj == this"));
		assertThat(code, anyOf(containsOne("obj == null"), containsOne("obj != null")));
	}

	@Test
	public void test2() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}
}
