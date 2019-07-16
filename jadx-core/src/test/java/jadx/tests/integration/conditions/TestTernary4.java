package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

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
		ClassNode cls = getClassNodeFromSmali();
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("r5")));
		assertThat(code, not(containsString("try")));
	}
}
