package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

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
		assertThat(getClassNodeFromSmali()).code()
				.containsOne("!list.isEmpty() && list.contains(this)");
	}
}
