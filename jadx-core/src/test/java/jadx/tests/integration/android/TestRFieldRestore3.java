package jadx.tests.integration.android;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestRFieldRestore3 extends IntegrationTest {

	public static class TestCls {

		@T(2131230730)
		public static class A {
			@F(2131230730)
			private int f;

			@M(bind = 2137373737)
			private void mth() {
			}

			@T(2137373737)
			private class D {
			}
		}

		@Retention(RetentionPolicy.RUNTIME)
		@Target(ElementType.TYPE)
		@interface T {
			int value();
		}

		@Retention(RetentionPolicy.RUNTIME)
		@Target({ ElementType.FIELD })
		@interface F {
			int value();
		}

		@Retention(RetentionPolicy.RUNTIME)
		@Target({ ElementType.METHOD })
		@interface M {
			int bind();
		}

		public static class R {
		}
	}

	@Test
	public void test() {
		Map<Integer, String> map = new HashMap<>();
		map.put(2131230730, "id.Button");
		map.put(2137373737, "id.MyId");
		setResMap(map);

		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOnlyOnce("@T(R.id.Button)")
				.containsOnlyOnce("@T(R.id.MyId)")
				.containsOnlyOnce("@F(R.id.Button)")
				.containsOnlyOnce("@M(bind = R.id.MyId)");
	}
}
