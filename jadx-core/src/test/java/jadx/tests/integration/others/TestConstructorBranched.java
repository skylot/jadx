package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

@SuppressWarnings("CommentedOutCode")
public class TestConstructorBranched extends SmaliTest {
	// @formatter:off
	/*
		public Set<String> test(Collection<String> collection) {
			Set<String> set;
			if (collection == null) {
				set = new HashSet<>();
			} else {
				set = new HashSet<>(collection);
			}
			set.add("end");
			return set;
		}
	 */
	// @formatter:on

	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("new HashSet()")
				.containsOne("new HashSet(collection)");
	}
}
