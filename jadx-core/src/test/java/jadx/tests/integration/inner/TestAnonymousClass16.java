package jadx.tests.integration.inner;

import java.util.HashMap;
import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.IntegrationTest;

public class TestAnonymousClass16 extends IntegrationTest {

	public static class TestCls {

		@SuppressWarnings("serial")
		public HashMap<String, String> test() {
			HashMap<String, String> map = new HashMap<String, String>() {
				{
					put("a", "b");
				}
			};
			map.put("c", "d");
			return map;
		}
	}


	@Test
	@NotYetImplemented
	public void test() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}
}

