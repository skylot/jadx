package jadx.tests.integration.invoke;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestCastInOverloadedAccessor extends SmaliTest {
	static class X {
		void test() {
			new Runnable() {
				@Override
				public void run() {
					outerMethod("");
					outerMethod("", "");
				}
			};
		}

		private void outerMethod(String s) {
		}

		private void outerMethod(String s, String t) {
		}

		private void outerMethod(int a) {
		}

		private void outerMethod(int a, int b) {
		}
	}

	@Test
	public void test() {
		String code = getClassNode(X.class).getCode().getCodeStr();
		assertThat(code, containsOne("outerMethod(\"\")"));
		assertThat(code, containsOne("outerMethod(\"\", \"\")"));
	}
}
