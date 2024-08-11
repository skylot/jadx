package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Issue: https://github.com/skylot/jadx/issues/397
 */
public class TestSyntheticMthRename extends SmaliTest {

	// @formatter:off
	/*
		public class TestCls {
			public interface I<R, P> {
				R call(P... p);
			}

			public static final class A implements I<String, Runnable> {
				public synthetic virtual Object call(Object[] objArr) {
					return renamedCall((Runnable[]) objArr);
				}

				private varargs direct String renamedCall(Runnable... p) {
					return "str";
				}
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		assertThat(getClassNodeFromSmaliFiles("inner", "TestSyntheticMthRename", "TestCls"))
				.code()
				.containsOne("public String call(Runnable... p) {")
				.doesNotContain("synthetic");
	}
}
