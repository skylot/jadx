package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTryCatch8 extends IntegrationTest {

	public static class TestCls {
		static class MyException extends Exception {
			private static final long serialVersionUID = 7963400419047287279L;

			MyException() {
			}

			MyException(String msg, Throwable cause) {
				super(msg, cause);
			}
		}

		MyException e = null;

		public void test() {
			synchronized (this) {
				try {
					throw new MyException();
				} catch (MyException myExc) {
					this.e = myExc;
				} catch (Exception ex) {
					this.e = new MyException("MyExc", ex);
				}
			}
		}

		public void check() {
			test();
			assertThat(e).isInstanceOf(MyException.class);
			assertThat(e.getMessage()).isNull();
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("synchronized (this) {")
				.containsOne("throw new MyException();")
				.containsOne("} catch (MyException myExc) {")
				.containsOne("this.e = myExc;")
				.containsOne("} catch (Exception ex) {")
				.containsOne("this.e = new MyException(\"MyExc\", ex);");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("synchronized (this) {")
				.containsOne("throw new MyException();");
	}
}
