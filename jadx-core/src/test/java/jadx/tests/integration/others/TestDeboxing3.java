package jadx.tests.integration.others;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestDeboxing3 extends IntegrationTest {

	public static class TestCls {

		public static class Pair<F, S> {
			public F first;
			public S second;
		}

		private Map<String, Pair<Long, String>> cache = new HashMap<>();

		public boolean test(String id, Long l) {
			if (l == null) {
				l = 900000L;
			}
			Pair<Long, String> pair = this.cache.get(id);
			if (pair == null) {
				return false;
			}
			return pair.first + l > System.currentTimeMillis();
		}
	}

	@Test
	public void test() {
		noDebugInfo();

		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("l = 900000L;"));
	}

	@Test
	@NotYetImplemented("Full deboxing and generics propagation")
	public void testFull() {
		noDebugInfo();

		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("Pair<Long, String> pair = this.cache.get(id);"));
		assertThat(code, containsOne("return pair.first + l > System.currentTimeMillis();"));
	}
}
