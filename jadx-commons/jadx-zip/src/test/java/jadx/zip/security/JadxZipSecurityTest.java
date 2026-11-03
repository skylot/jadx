package jadx.zip.security;

import java.io.File;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import jadx.zip.IZipEntry;

import static org.assertj.core.api.Assertions.assertThat;

class JadxZipSecurityTest {

	@Test
	void testPaddedEntryIsNotAZipBomb() {
		// Padded binary XML from protected APKs: 157 MB unpacked from 157 KB,
		// a high ratio but a valid and small enough file
		JadxZipSecurity security = new JadxZipSecurity();
		assertThat(security.isZipBomb(new TestEntry(157L * 1024 * 1024, 157L * 1024))).isFalse();
	}

	@Test
	void testBigEntryWithNormalRatioIsNotAZipBomb() {
		// Split APK file inside an APKS/XAPK archive: bigger than the size limit,
		// but compressed with a normal ratio, so it is not a bomb
		JadxZipSecurity security = new JadxZipSecurity();
		assertThat(security.isZipBomb(new TestEntry(800L * 1024 * 1024, 400L * 1024 * 1024))).isFalse();
	}

	@Test
	void testBigAndHighlyCompressedEntryIsAZipBomb() {
		JadxZipSecurity security = new JadxZipSecurity();
		assertThat(security.isZipBomb(new TestEntry(1024L * 1024 * 1024, 1024 * 1024))).isTrue();
	}

	@Test
	void testInvalidSizeIsRejected() {
		JadxZipSecurity security = new JadxZipSecurity();
		assertThat(security.isZipBomb(new TestEntry(-1, 1024))).isTrue();
	}

	@Test
	void testMaxUncompressedEntrySizeCanBeChanged() {
		JadxZipSecurity security = new JadxZipSecurity();
		security.setMaxUncompressedEntrySize(50L * 1024 * 1024);
		assertThat(security.isZipBomb(new TestEntry(60L * 1024 * 1024, 1024))).isTrue();
		assertThat(security.isZipBomb(new TestEntry(60L * 1024 * 1024, 50L * 1024 * 1024))).isFalse();
	}

	private static final class TestEntry implements IZipEntry {
		private final long uncompressedSize;
		private final long compressedSize;

		TestEntry(long uncompressedSize, long compressedSize) {
			this.uncompressedSize = uncompressedSize;
			this.compressedSize = compressedSize;
		}

		@Override
		public String getName() {
			return "test";
		}

		@Override
		public byte[] getBytes() {
			throw new UnsupportedOperationException();
		}

		@Override
		public InputStream getInputStream() {
			throw new UnsupportedOperationException();
		}

		@Override
		public long getCompressedSize() {
			return compressedSize;
		}

		@Override
		public long getUncompressedSize() {
			return uncompressedSize;
		}

		@Override
		public boolean isDirectory() {
			return false;
		}

		@Override
		public File getZipFile() {
			return new File("test.zip");
		}

		@Override
		public boolean preferBytes() {
			return true;
		}
	}
}
