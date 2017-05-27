package jadx.tests.integration.inner;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class TestRFieldRestore extends IntegrationTest {

	public static class TestCls {
		public int test() {
			return 2131230730;
		}
	}

	@Test
	public void test() {
		// unknown R class
		disableCompilation();

		Map<Integer, String> map = new HashMap<>();
		map.put(2131230730, "id.Button");
		setResMap(map);

		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		assertThat(code, containsOne("return R.id.Button;"));
		assertThat(code, not(containsString("import R;")));
	}
}
