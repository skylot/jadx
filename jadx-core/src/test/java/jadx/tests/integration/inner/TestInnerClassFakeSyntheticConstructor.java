package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestInnerClassFakeSyntheticConstructor extends SmaliTest {

	// @formatter:off
	/*
		public class TestCls {
			public synthetic TestCls(String a) {
				this(a, true);
			}

			public TestCls(String a, boolean b) {
			}

			public static TestCls build(String str) {
				return new TestCls(str);
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		assertThat(getClassNodeFromSmali("inner/TestInnerClassFakeSyntheticConstructor", "jadx.tests.inner.TestCls"))
				.code()
				.containsOne("TestCls(String a) {");
		// and must compile
	}
}
