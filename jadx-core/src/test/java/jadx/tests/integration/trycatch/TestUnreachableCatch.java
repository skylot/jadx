package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

@SuppressWarnings("CommentedOutCode")
public class TestUnreachableCatch extends SmaliTest {

	// @formatter:off
	/*
		private static Map<Uri, ByteBuffer> prepareFontData(Context context, FontInfo[] fonts,
			CancellationSignal cancellationSignal) {
		final HashMap<Uri, ByteBuffer> out = new HashMap<>();
		final ContentResolver resolver = context.getContentResolver();

		for (FontInfo font : fonts) {
			if (font.getResultCode() != Columns.RESULT_CODE_OK) {
				continue;
			}

			final Uri uri = font.getUri();
			if (out.containsKey(uri)) {
				continue;
			}

			ByteBuffer buffer = null;
			try (final ParcelFileDescriptor pfd =
					resolver.openFileDescriptor(uri, "r", cancellationSignal)) {
				if (pfd != null) {
					try (final FileInputStream fis =
							new FileInputStream(pfd.getFileDescriptor())) {
						final FileChannel fileChannel = fis.getChannel();
						final long size = fileChannel.size();
						buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, size);
					} catch (IOException e) {
						// ignore
					}
				}
			} catch (IOException e) {
				// ignore
			}

			// TODO: try other approach?, e.g. read all contents instead of mmap.

			out.put(uri, buffer);
		}
		return Collections.unmodifiableMap(out);
	}

	*/
	// @formatter:on

	@Test
	public void test() {
		disableCompilation();
		allowWarnInCode();
		assertThat(getClassNodeFromSmali())
				.code()
				.contains("IOException")
				.contains("Collections.unmodifiableMap");
	}
}
