package jadx.gui.utils;

import java.io.IOException;
import java.io.InputStream;

import org.jetbrains.annotations.Nullable;

public class IOUtils {

	/**
	 * This method can be deleted once Jadx is Java11+
	 */
	@Nullable
	public static byte[] readNBytes(InputStream inputStream, int len) throws IOException {
		byte[] payload = new byte[len];
		int readSize = 0;
		while (true) {
			int read = inputStream.read(payload, readSize, len - readSize);
			if (read == -1) {
				return null;
			}
			readSize += read;
			if (readSize == len) {
				return payload;
			}
		}
	}

	public static int read(InputStream inputStream, byte[] buf) throws IOException {
		return read(inputStream, buf, 0, buf.length);
	}

	public static int read(InputStream inputStream, byte[] buf, int off, int len) throws IOException {
		int remainingBytes = len;
		while (remainingBytes > 0) {
			int start = len - remainingBytes;
			int bytesRead = inputStream.read(buf, off + start, remainingBytes);
			if (bytesRead == -1) {
				break;
			}
			remainingBytes -= bytesRead;
		}
		return len - remainingBytes;
	}
}
