package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestNullInline extends IntegrationTest {

	@SuppressWarnings({ "RedundantCast", "DataFlowIssue", "unused" })
	public static class TestCls {
		public static Long test(Double d1) {
			T1<T2, Byte> t1 = (T1<T2, Byte>) null;
			return t1.t2.l;
		}

		static class T2 {
			public long l;
		}

		static class T1<H, P extends Byte> {
			public T2 t2;

			public T1(T2 t2) {
				this.t2 = t2;
			}
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("Long.valueOf(t1.t2.l);");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}
}
