package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.api.CommentsLevel;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestAnonymousClass16 extends IntegrationTest {

	public static class TestCls {

		public Something test() {
			Something a = new Something() {
				{
					put("a", "b");
				}
			};
			a.put("c", "d");
			return a;
		}

		public class Something {
			public void put(Object o, Object o2) {
			}
		}
	}

	@Test
	public void test() {
		getArgs().setCommentsLevel(CommentsLevel.NONE);
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("r0")
				.doesNotContain("AnonymousClass1 r0 = ")
				.containsLines(2,
						"Something something = new Something() {",
						indent() + "{",
						indent(2) + "put(\"a\", \"b\");",
						indent() + "}",
						"};");
	}
}
