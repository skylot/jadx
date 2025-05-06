package jadx.tests.integration.others;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestThrows extends IntegrationTest {

	public static class MissingThrowsTest extends Exception {

		private void throwCustomException() throws MissingThrowsTest {
			throw new MissingThrowsTest();
		}

		private void throwException() throws Exception {
			throw new Exception();
		}

		private void throwRuntimeException1() {
			throw new RuntimeException();
		}

		private void throwRuntimeException2() {
			throw new NullPointerException();
		}

		private void throwError() {
			throw new Error();
		}

		private void throwError2() {
			throw new OutOfMemoryError();
		}

		@SuppressWarnings("checkstyle:illegalThrows")
		private void throwThrowable() throws Throwable {
			throw new Throwable();
		}

		private void exceptionSource() throws FileNotFoundException {
			throw new FileNotFoundException("");
		}

		public void mergeThrownExcetions() throws IOException {
			exceptionSource();
		}

		public void rethrowThrowable() {
			try {
			} catch (Throwable t) {
				throw t;
			}
		}

		public void doSomething1(int i) throws FileNotFoundException {
			if (i == 1) {
				doSomething2(i);
			} else {
				doSomething1(i);
			}
		}

		public void doSomething2(int i) throws FileNotFoundException {
			if (i == 1) {
				exceptionSource();
			} else {
				doSomething1(i);
			}
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(MissingThrowsTest.class))
				.code()
				.contains("throwCustomException() throws TestThrows$MissingThrowsTest {", "throwException() throws Exception {",
						"throwRuntimeException1() {", "throwRuntimeException2() {",
						"throwError() {", "throwError2() {", "throwThrowable() throws Throwable {",
						"exceptionSource() throws FileNotFoundException {", "mergeThrownExcetions() throws IOException {",
						"rethrowThrowable() {");
	}
}
