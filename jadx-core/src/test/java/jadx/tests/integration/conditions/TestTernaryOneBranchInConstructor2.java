package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTernaryOneBranchInConstructor2 extends SmaliTest {

	// @formatter:off
	/*
		public class A {
			public A(String str, String str2, String str3, boolean z) {}

			public A(String str, String str2, String str3, boolean z, int i, int i2) {
				this(str, (i & 2) != 0 ? "" : str2, (i & 4) != 0 ? "" : str3, (i & 8) != 0 ? false : z);
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("this(str, (i & 2) != 0 ? \"\" : str2, (i & 4) != 0 ? \"\" : str3, (i & 8) != 0 ? false : z);")
				.doesNotContain("//");
	}
}
