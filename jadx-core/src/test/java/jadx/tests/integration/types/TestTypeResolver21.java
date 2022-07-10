package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Issue 1527
 */
@SuppressWarnings("CommentedOutCode")
public class TestTypeResolver21 extends SmaliTest {
	// @formatter:off
	/*
		public Number test(Object objectArray) {
			Object[] arr = (Object[]) objectArray;
			return (Number) arr[0];
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("Object[] arr = (Object[]) objectArray;");
	}
}
