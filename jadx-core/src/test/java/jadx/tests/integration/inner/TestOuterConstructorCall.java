package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.api.CommentsLevel;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestOuterConstructorCall extends IntegrationTest {

	@SuppressWarnings({ "InnerClassMayBeStatic", "unused" })
	public static class TestCls {
		private TestCls(Inner inner) {
			System.out.println(inner);
		}

		private class Inner {
			private TestCls test() {
				return new TestCls(this);
			}
		}
	}

	@Test
	public void test() {
		getArgs().setCommentsLevel(CommentsLevel.WARN);
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("class Inner {")
				.containsOne("return new TestOuterConstructorCall$TestCls(this);")
				.doesNotContain("synthetic", "this$0");
	}
}
