package jadx.tests.integration.trycatch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("decode(inputStream);"));
		assertThat(code, containsOne("return new Result(400);"));
		assertThat(code, not(containsOne("result =")));
	}
}
