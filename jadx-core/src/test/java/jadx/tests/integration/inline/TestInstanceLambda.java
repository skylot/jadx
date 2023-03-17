package jadx.tests.integration.inline;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestInstanceLambda extends SmaliTest {

	@SuppressWarnings({ "unchecked", "rawtypes", "SameParameterValue" })
	public static class TestCls {

		public <T> Map<T, T> test(List<? extends T> list) {
			return toMap(list, Lambda.INSTANCE);
		}

		/**
		 * Smali test missing 'T' definition in 'Lambda<T>'
		 */
		private static class Lambda<T> implements Function<T, T> {
			public static final Lambda INSTANCE = new Lambda();

			@Override
			public T apply(T t) {
				return t;
			}
		}

		private static <T> Map<T, T> toMap(List<? extends T> list, Function<T, T> valueMap) {
			return list.stream().collect(Collectors.toMap(k -> k, valueMap));
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code();
	}

	@Test
	public void testSmaliDisableInline() {
		args.setInlineAnonymousClasses(false);
		List<ClassNode> classNodes = loadFromSmaliFiles();
		assertThat(searchTestCls(classNodes, "Lambda"))
				.code()
				.containsOne("class Lambda<T> implements Function<T, T> {");
		assertThat(searchTestCls(classNodes, "TestCls"))
				.code()
				.containsOne("Lambda.INSTANCE");
	}

	@NotYetImplemented("Inline lambda by instance field")
	@Test
	public void testSmali() {
		List<ClassNode> classNodes = loadFromSmaliFiles();
		assertThat(classNodes)
				.describedAs("Expect lambda to be inlined")
				.hasSize(1);
		assertThat(searchTestCls(classNodes, "TestCls"))
				.code()
				.doesNotContain("Lambda.INSTANCE");
	}
}
