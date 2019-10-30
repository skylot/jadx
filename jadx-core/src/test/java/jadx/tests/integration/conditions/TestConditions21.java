package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestConditions21 extends SmaliTest {

	// @formatter:off
	/*
		public boolean check(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj instanceof List) {
				List list = (List) obj;
				if (!list.isEmpty() && list.contains(this)) {
					return true;
				}
			}
			return false;
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		ClassNode cls = getClassNodeFromSmali();
		String code = cls.getCode().toString();

		assertThat(code, containsOne("!list.isEmpty() && list.contains(this)"));
	}
}
