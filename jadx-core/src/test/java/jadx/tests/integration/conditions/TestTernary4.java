package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

@SuppressWarnings("CommentedOutCode")
public class TestTernary4 extends SmaliTest {

	// @formatter:off
	/*
		private Set test(HashMap<String, Object> hashMap) {
			boolean z;
			HashSet hashSet = new HashSet();
			synchronized (this.defaultValuesByPath) {
				for (String next : this.defaultValuesByPath.keySet()) {
					Object obj = hashMap.get(next);
					if (obj != null) {
						z = !getValueObject(next).equals(obj);
					} else {
						z = this.valuesByPath.get(next) != null;;
					}
					if (z) {
						hashSet.add(next);
					}
				}
			}
			return hashSet;
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.removeBlockComments()
				.doesNotContain("5")
				.doesNotContain("try");
	}
}
