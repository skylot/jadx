package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("checkstyle:printstacktrace")
public class TestTryCatchFinally extends IntegrationTest {

	public static class TestCls {
		public boolean f;

		@SuppressWarnings("ConstantConditions")
		private boolean test(Object obj) {
			this.f = false;
			try {
				exc(obj);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				this.f = true;
			}
			return this.f;
		}

		private static boolean exc(Object obj) throws Exception {
			if (obj == null) {
				throw new Exception("test");
			}
			return (obj instanceof String);
		}

		public void check() {
			assertTrue(test("a"));
			assertTrue(test(null));
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("this.f = false;")
				.containsOne("exc(obj);")
				.containsOne("} catch (Exception e) {")
				.containsOne("e.printStackTrace();")
				.containsOne("} finally {")
				.containsOne("this.f = true;")
				.containsOne("return this.f;")
				.doesNotContain("boolean z");
	}

	@Test
	public void testWithoutFinally() {
		args.setExtractFinally(false);
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("exc(obj);")
				.containsOne(indent(3) + "} catch (Exception e) {")
				.containsOne(indent(2) + "} catch (Throwable th) {")
				.containsOne("this.f = false;")
				.countString(3, "this.f = true;");
	}
}
