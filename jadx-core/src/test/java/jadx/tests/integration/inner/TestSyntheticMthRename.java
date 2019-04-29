package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

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
		ClassNode cls = getClassNodeFromSmaliFiles("inner", "TestSyntheticMthRename", "TestCls");
		String code = cls.getCode().toString();

		assertThat(code, containsOne("public String call(Runnable... p) {"));
		assertThat(code, not(containsString("synthetic")));
	}
}
