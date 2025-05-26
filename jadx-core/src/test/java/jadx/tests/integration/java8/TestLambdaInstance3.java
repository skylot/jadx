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
		// TODO: type inference set type for 'lazyOf' to Memoized and cast incorrectly removed
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
