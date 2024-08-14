package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

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
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("for (Map.Entry<Integer, String> entry : map.entrySet()) {")
				.containsOne("useInt(entry.getKey().intValue());") // no Integer cast
				.containsOne("entry.getValue().trim();"); // no String cast
	}
}
