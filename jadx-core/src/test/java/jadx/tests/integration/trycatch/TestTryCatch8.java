package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

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
			assertThat(e, notNullValue());
			assertThat(e, isA(MyException.class));
			assertThat(e.getMessage(), nullValue());
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("synchronized (this) {"));
		assertThat(code, containsOne("throw new MyException();"));
		assertThat(code, containsOne("} catch (MyException myExc) {"));
		assertThat(code, containsOne("this.e = myExc;"));
		assertThat(code, containsOne("} catch (Exception ex) {"));
		assertThat(code, containsOne("this.e = new MyException(\"MyExc\", ex);"));
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("synchronized (this) {"));
		assertThat(code, containsOne("throw new MyException();"));
	}
}
