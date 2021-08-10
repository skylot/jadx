package jadx.tests.integration.trycatch;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestFinally3 extends SmaliTest {

	@SuppressWarnings({ "RedundantThrows", "unused" })
	public static class TestCls {
		public byte[] bytes;

		public byte[] test() throws Exception {
			InputStream inputStream = null;
			try {
				if (bytes == null) {
					if (!validate()) {
						return null;
					}
					inputStream = getInputStream();
					bytes = read(inputStream);
				}
				return convert(bytes);
			} finally {
				close(inputStream);
			}
		}

		private byte[] convert(byte[] bytes) throws Exception {
			return new byte[0];
		}

		private boolean validate() throws Exception {
			return false;
		}

		private InputStream getInputStream() throws Exception {
			return new ByteArrayInputStream(new byte[] {});
		}

		private byte[] read(InputStream in) throws Exception {
			return new byte[] {};
		}

		private static void close(InputStream is) {
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("} finally {")
				.doesNotContain("close(null);")
				.containsOne("close(inputStream);");
	}

	@NotYetImplemented("Finally extract failed")
	@Test
	public void test2NoDebug() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("} finally {")
				.containsOne(indent() + "close(");
	}

	@Test
	public void testSmali() {
		assertThat(getClassNodeFromSmali())
				.code()
				.doesNotContain("Type inference failed")
				.containsOne("} finally {");
	}
}
