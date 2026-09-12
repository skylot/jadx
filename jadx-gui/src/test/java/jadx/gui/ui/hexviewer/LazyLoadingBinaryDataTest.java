package jadx.gui.ui.hexviewer;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LazyLoadingBinaryDataTest {

	private static final int BLOCK_SIZE = 1024 * 512;

	@Test
	void getByteLoadsByteAtBlockBoundary() {
		byte[] source = new byte[BLOCK_SIZE + 2];
		source[BLOCK_SIZE - 1] = 1;
		source[BLOCK_SIZE] = 2;
		source[BLOCK_SIZE + 1] = 3;
		LazyLoadingBinaryData data = new LazyLoadingBinaryData(new ByteArrayInputStream(source), source.length);

		assertThat(data.getByte(BLOCK_SIZE - 1)).isEqualTo((byte) 1);
		assertThat(data.getByte(BLOCK_SIZE)).isEqualTo((byte) 2);
		assertThat(data.getByte(BLOCK_SIZE + 1)).isEqualTo((byte) 3);
	}

	@Test
	void getByteLoadsByteAtBlockBoundaryForStreamWithUnknownSize() {
		byte[] source = new byte[BLOCK_SIZE + 2];
		source[BLOCK_SIZE] = 2;
		LazyLoadingBinaryData data = new LazyLoadingBinaryData(
				new FilterInputStream(new ByteArrayInputStream(source)) {
					@Override
					public int available() throws IOException {
						return -1;
					}
				},
				0);

		assertThat(data.getByte(BLOCK_SIZE)).isEqualTo((byte) 2);
	}

	@Test
	void getByteRejectsPositionsOutsideData() {
		byte[] source = new byte[BLOCK_SIZE + 1];
		Arrays.fill(source, (byte) 7);
		LazyLoadingBinaryData data = new LazyLoadingBinaryData(new ByteArrayInputStream(source), source.length);

		assertThatThrownBy(() -> data.getByte(-1)).isInstanceOf(IndexOutOfBoundsException.class);
		assertThatThrownBy(() -> data.getByte(source.length)).isInstanceOf(IndexOutOfBoundsException.class);
	}
}
