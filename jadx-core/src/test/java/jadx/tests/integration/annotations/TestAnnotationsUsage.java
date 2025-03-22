package jadx.tests.integration.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestAnnotationsUsage extends IntegrationTest {

	public static class TestCls {

		@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
		@Retention(RetentionPolicy.RUNTIME)
		public @interface A {
			Class<?> c();
		}

		@A(c = TestCls.class)
		public static class B {
		}

		public static class C {
			@A(c = B.class)
			public String field;
		}

		@A(c = B.class)
		void test() {
		}

		void test2(@A(c = B.class) Integer value) {
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		ClassNode annCls = searchCls(cls.getInnerClasses(), "A");
		ClassNode bCls = searchCls(cls.getInnerClasses(), "B");
		ClassNode cCls = searchCls(cls.getInnerClasses(), "C");
		MethodNode testMth = getMethod(cls, "test");
		MethodNode testMth2 = getMethod(cls, "test2");

		assertThat(annCls.getUseIn()).contains(cls, bCls, cCls);
		assertThat(annCls.getUseInMth()).contains(testMth, testMth2);

		assertThat(bCls.getUseIn()).contains(cCls);
		assertThat(bCls.getUseInMth()).contains(testMth, testMth2);

		assertThat(cls)
				.code()
				.countString(3, "@A(c = B.class)");
	}
}
