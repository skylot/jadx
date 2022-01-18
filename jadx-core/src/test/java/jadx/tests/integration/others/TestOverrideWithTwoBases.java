package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestOverrideWithTwoBases extends IntegrationTest {

	public static class TestCls {
		public abstract static class BaseClass {
			public abstract int a();
		}

		public interface I {
			int a();
		}

		public static class Cls extends BaseClass implements I {
			@Override
			public int a() {
				return 2;
			}
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("@Override");
	}
}
