package jadx.tests.integration.names;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestCollisionWithJavaLangClasses extends IntegrationTest {

	public static class TestCls1 {
		public static class System {
			public static void main(String[] args) {
				java.lang.System.out.println("Hello world");
			}
		}
	}

	@Test
	public void test1() {
		assertThat(getClassNode(TestCls1.class))
				.code()
				.containsOne("java.lang.System.out.println");
	}

	public static class TestCls2 {
		public void doSomething() {
			System.doSomething();
			java.lang.System.out.println("Hello World");
		}

		public static class System {
			public static void doSomething() {
			}
		}
	}

	@Test
	public void test2() {
		assertThat(getClassNode(TestCls2.class))
				.code()
				.containsLine(2, "System.doSomething();")
				.containsOne("java.lang.System.out.println");
	}

	@Test
	public void test3() {
		List<ClassNode> classes = getClassNodes(
				jadx.tests.integration.names.pkg2.System.class,
				jadx.tests.integration.names.pkg2.TestCls.class);
		assertThat(searchCls(classes, "TestCls"))
				.code()
				.containsLine(2, "System.doSomething();")
				.containsOne("java.lang.System.out.println");
	}
}
