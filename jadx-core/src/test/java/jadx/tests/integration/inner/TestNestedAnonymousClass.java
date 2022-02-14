package jadx.tests.integration.inner;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestNestedAnonymousClass extends SmaliTest {

	@SuppressWarnings("Convert2Lambda")
	public static class TestCls {
		public void test() {
			use(new Callable<Runnable>() {
				@Override
				public Runnable call() {
					return new Runnable() {
						@Override
						public void run() {
							System.out.println("run");
						}
					};
				}
			});
		}

		public void testLambda() {
			use(() -> () -> System.out.println("lambda"));
		}

		public void use(Callable<Runnable> r) {
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("use(new Callable<Runnable>() {")
				.containsOne("return new Runnable() {");
	}

	@Test
	public void testSmali() {
		getArgs().setRenameFlags(Collections.emptySet());
		List<ClassNode> classes = loadFromSmaliFiles();
		assertThat(searchCls(classes, "A"))
				.code()
				.containsOne("use(new Callable<Runnable>() {")
				.containsOne("return new Runnable() {");
	}
}
