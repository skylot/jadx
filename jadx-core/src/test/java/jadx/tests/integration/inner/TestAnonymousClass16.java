package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.IntegrationTest;

public class TestAnonymousClass16 extends IntegrationTest {

	public static class TestCls {

		public Something test() {
			Something a = new Something() {
				{
					put("a", "b");
				}
			};
			a.put("c", "d");
			return a;
		}

		public class Something {
			public void put(Object o, Object o2) {
			}
		}
	}

	@Test
	@NotYetImplemented
	public void test() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}
}
