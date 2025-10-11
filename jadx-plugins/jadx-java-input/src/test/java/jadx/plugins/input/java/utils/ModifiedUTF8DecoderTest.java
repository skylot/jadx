package jadx.plugins.input.java.utils;

import org.junit.jupiter.api.Test;

import static jadx.plugins.input.java.utils.ModifiedUTF8Decoder.decodeString;
import static org.assertj.core.api.Assertions.assertThat;

/*
 * TODO: find a way to enter 6-bytes char decode branch
 */
class ModifiedUTF8DecoderTest {

	@Test
	public void test() {
		String str = "aÆřᛒቶ北𝄠😀🨄𐆙";
		byte[] mUTF8Bytes = new byte[] { 97, -61, -122, -59, -103, -31, -101, -110, -31, -119, -74, -17,
				-91, -93, -19, -96, -76, -19, -76, -96, -19, -96, -67, -19, -72,
				-128, -19, -96, -66, -19, -72, -124, -19, -96, -128, -19, -74, -103 };
		assertThat(decodeString(mUTF8Bytes)).isEqualTo(str);
	}

	@Test
	public void testASCIIOnly() {
		String str = "Hello, world!";
		byte[] mUTF8Bytes = new byte[] { 72, 101, 108, 108, 111, 44, 32, 119, 111, 114, 108, 100, 33 };
		assertThat(decodeString(mUTF8Bytes)).isEqualTo(str);
	}

}
