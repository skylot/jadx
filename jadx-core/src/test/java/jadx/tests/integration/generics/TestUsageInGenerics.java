package jadx.tests.integration.generics;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestUsageInGenerics extends IntegrationTest {

	public static class TestCls {

		public static class A {
		}

		public static class B<T extends A> {
		}

		public static class C {
			public List<? extends A> list;
		}

		public <T extends A> T test() {
			return null;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		ClassNode testCls = searchCls(cls.getInnerClasses(), "A");
		ClassNode bCls = searchCls(cls.getInnerClasses(), "B");
		ClassNode cCls = searchCls(cls.getInnerClasses(), "C");
		MethodNode testMth = getMethod(cls, "test");

		assertThat(testCls.getUseIn()).contains(cls, bCls, cCls);
		assertThat(testCls.getUseInMth()).contains(testMth);

		assertThat(cls)
				.code()
				.containsOne("public <T extends A> T test() {");
	}
}
