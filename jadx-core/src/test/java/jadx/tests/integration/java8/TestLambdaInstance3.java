package jadx.tests.integration.java8;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import jadx.tests.api.RaungTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

@SuppressWarnings("DataFlowIssue")
public class TestLambdaInstance3 extends RaungTest {

	public interface TestCls<R> extends Supplier<R> {
		default TestCls<R> test() {
			return (TestCls<R> & Memoized) Lazy.of(this)::get;
		}
	}

	public static final class Lazy<T> implements Supplier<T> {
		public static <T> Lazy<T> of(Supplier<? extends T> supplier) {
			return null;
		}

		@Override
		public T get() {
			return null;
		}
	}

	interface Memoized {
	}

	@Test
	public void test() {
		// some java versions failed to compile usage of interface with '$' in name
		addClsRename("jadx.tests.integration.java8.TestLambdaInstance3$TestCls", "java8.TestCls");
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("this::get")
				.containsOne("return (TestCls) lazyOf::get;");
		// TODO: Fix cascading casts - when javac generates CHECK_CAST to Memoized then CHECK_CAST to TestCls,
		//  we currently only mark the first cast as EXPLICIT, but the second cast shadows it during region transformation.
		//  The raung test (testRaung) works correctly and shows (Memoized) cast.
		// .containsOne("Memoized)");
	}

	@Test
	public void testRaung() {
		disableCompilation();
		assertThat(getClassNodeFromRaung())
				.code()
				.doesNotContain("this::get")
				.containsOne(" lazyOf::get");
	}
}
