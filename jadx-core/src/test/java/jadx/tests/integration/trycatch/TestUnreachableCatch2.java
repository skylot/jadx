package jadx.tests.integration.trycatch;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestUnreachableCatch2 extends SmaliTest {

	@SuppressWarnings({ "unused", "DataFlowIssue" })
	public static class UnusedExceptionHandlers1 implements AutoCloseable {
		public static void test(final Object unused1, final Object[] array, final Object o1,
				final Object o2, final Object unused2) {
			for (final Object item : array) {
				ByteBuffer buffer = null;
				try (final UnusedExceptionHandlers1 u = doSomething2(o1, "", o2)) {
					try (final FileInputStream fis = new FileInputStream(u.getFilename())) {
						final FileChannel fileChannel = fis.getChannel();
						buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, 42);
					} catch (IOException e) {
						// ignore
					}
				} catch (IOException e) {
					// ignore
				}
			}
		}

		private String getFilename() {
			return null;
		}

		private static UnusedExceptionHandlers1 doSomething2(final Object o1, final String s,
				final Object o2) {
			return null;
		}

		@Override
		public void close() throws IOException {
		}
	}

	@Test
	public void test() {
		// TODO: result code not compilable because 'try' block split into 2 block and 'fis' var become
		// uninitialized
		disableCompilation();
		assertThat(getClassNode(UnusedExceptionHandlers1.class))
				.code()
				.doesNotContain("break;")
				.countString(2, "} catch (IOException e");
	}
}
