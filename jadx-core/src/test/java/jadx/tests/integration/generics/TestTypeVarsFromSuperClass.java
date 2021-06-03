package jadx.tests.integration.generics;

import java.util.Objects;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTypeVarsFromSuperClass extends IntegrationTest {

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static class TestCls {

		public static class C1<A> {
		}

		public static class C2<B> extends C1<B> {
			public B call() {
				return null;
			}
		}

		public static class C3<C> extends C2<C> {
		}

		public static class C4 extends C3<String> {
			public Object test() {
				String str = call();
				Objects.nonNull(str);
				return str;
			}
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("= call();")
				.doesNotContain("(String)");
	}
}
