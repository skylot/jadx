package jadx.tests.integration.android;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestRFieldRestore2 extends IntegrationTest {

	public static class TestCls {

		public static class R {
		}

		public int test() {
			return 2131230730;
		}
	}

	@Test
	public void test() {
		Map<Integer, String> map = new HashMap<>();
		map.put(2131230730, "id.Button");
		setResMap(map);

		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("R.id.Button;");
	}
}
