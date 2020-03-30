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
			for (Map.Entry<Integer, String> entry : map.entrySet()) {
				useInt(entry.getKey().intValue());
				entry.getValue().trim();
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		ClassNode cls = getClassNodeFromSmali();
		String code = cls.getCode().toString();

		assertThat(code, containsOne("for (Map.Entry<Integer, String> entry : map.entrySet()) {"));
		assertThat(code, containsOne("useInt(entry.getKey().intValue());")); // no Integer cast
		assertThat(code, containsOne("entry.getValue().trim();")); // no String cast
	}
}
