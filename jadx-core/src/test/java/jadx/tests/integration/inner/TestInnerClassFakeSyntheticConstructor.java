package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

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
		ClassNode cls = getClassNodeFromSmali("inner/TestInnerClassFakeSyntheticConstructor", "jadx.tests.inner.TestCls");
		String code = cls.getCode().toString();

		assertThat(code, containsOne("TestCls(String a) {"));
		// and must compile
	}
}
