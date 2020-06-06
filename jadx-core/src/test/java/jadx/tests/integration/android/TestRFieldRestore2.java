package jadx.tests.integration.android;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

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

		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		assertThat(code, containsOne("R.id.Button;"));
	}
}
