package jadx.tests.integration.trycatch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestFinally2 extends IntegrationTest {

	public static class TestCls {

		public Result test(byte[] data) throws IOException {
			InputStream inputStream = null;
			try {
				inputStream = getInputStream(data);
				decode(inputStream);
				return new Result(400);
			} finally {
				closeQuietly(inputStream);
			}
		}

		public static final class Result {
			private final int mCode;

			public Result(int code) {
				mCode = code;
			}

			public int getCode() {
				return mCode;
			}
		}

		private InputStream getInputStream(byte[] data) throws IOException {
			return new ByteArrayInputStream(data);
		}

		private int decode(InputStream inputStream) throws IOException {
			return inputStream.available();
		}

		private void closeQuietly(InputStream is) {
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("decode(inputStream);")
				.containsOne("return new Result(400);")
				.doesNotContain("result =");
	}
}
