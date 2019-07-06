package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestGenerics2 extends SmaliTest {

	// @formatter:off
	/*
		public void test() {
			Map<Integer, String> map = this.field;
			useInt(map.size());
			Iterator<Map.Entry<Integer, String>> it = map.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Integer, String> next = it.next();
				useInt(next.getKey().intValue());
				next.getValue().trim();
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		ClassNode cls = getClassNodeFromSmali();
		String code = cls.getCode().toString();

		assertThat(code, containsOne("Entry<Integer, String> next"));
		assertThat(code, containsOne("useInt(next.getKey().intValue());")); // no Integer cast
	}
}
