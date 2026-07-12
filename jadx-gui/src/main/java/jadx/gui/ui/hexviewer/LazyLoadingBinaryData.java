package jadx.gui.ui.hexviewer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.exbin.auxiliary.binary_data.BinaryData;
import org.exbin.auxiliary.binary_data.array.ByteArrayData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LazyLoadingBinaryData implements BinaryData {

	private static final Logger LOG = LoggerFactory.getLogger(LazyLoadingBinaryData.class);

	private final int blockSize = 1024 * 512; // 512 KB

	private final InputStream inputStream;

	private long size;

	private long readPos = 0;

	private boolean endOfStreamReached = false;

	/**
	 * List of loaded byte array blocks. Each block has a length of BLOCK_SIZE except for the last
	 * block.
	 */
	private final List<byte[]> blocks = new ArrayList<>();

	public LazyLoadingBinaryData(InputStream inputStream, long expectedSize) {
		this.inputStream = inputStream;
		this.size = expectedSize;
		try {
			long available = inputStream.available();
			if (available > expectedSize) {
				this.size = available;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		loadBlock();
	}

	private boolean ensurePositionLoaded(long position) {
		while (!endOfStreamReached && readPos < position) {
			loadBlock();
		}
		return readPos >= position;
	}

	private synchronized void loadBlock() {
		if (endOfStreamReached) {
			return;
		}
		try {
			byte[] block = new byte[blockSize];
			int bytesRead = inputStream.readNBytes(block, 0, blockSize);
			boolean lastBlock = bytesRead < blockSize;
			if (bytesRead > 0) {
				if (lastBlock) {
					byte[] newBlock = new byte[bytesRead];
					System.arraycopy(block, 0, newBlock, 0, bytesRead);
					block = newBlock;
				}
				blocks.add(block);
				readPos += bytesRead;
				if (readPos >= size) {
					size = readPos;
				}
			}
			if (lastBlock) {
				endOfStreamReached = true;
			}
			LOG.trace("loaded {} bytes - readPos={} size={} endOfStreamReached={}", bytesRead, readPos, size, endOfStreamReached);
		} catch (IOException e) {
			endOfStreamReached = true;
			LOG.error("Error reading from input stream", e);
		}
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public long getDataSize() {
		return size;
	}

	@Override
	public byte getByte(long position) {
		if (!ensurePositionLoaded(position)) {
			throw new RuntimeException("Unreachable position: " + position);
		}
		int blockNum = (int) (position / blockSize);
		int blockOffset = (int) (position % blockSize);
		return blocks.get(blockNum)[blockOffset];
	}

	@Override
	public @NotNull BinaryData copy() {
		throw new NotImplementedException();
	}

	@Override
	public @NotNull BinaryData copy(long startFrom, long length) {
		byte[] data = new byte[(int) length];
		copyToArray(startFrom, data, 0, (int) length);
		return new ByteArrayData(data);
	}

	@Override
	public void copyToArray(long startFrom, byte[] target, int offset, int length) {
		long endPosition = startFrom + length;
		if (!ensurePositionLoaded(endPosition)) {
			throw new RuntimeException("Unreachable position: " + endPosition);
		}
		int blockNum = (int) (startFrom / blockSize);
		int blockOffset = (int) (startFrom % blockSize);
		int remaining = length;
		int targetPos = offset;
		while (remaining > 0) {
			byte[] block = blocks.get(blockNum);
			int copyLength = Math.min(remaining, block.length - blockOffset);
			System.arraycopy(block, blockOffset, target, targetPos, copyLength);
			remaining -= copyLength;
			targetPos += copyLength;
			blockNum++;
			blockOffset = 0;
		}
	}

	@Override
	public void saveToStream(OutputStream outputStream) {
		throw new UnsupportedOperationException();
	}

	@Override
	public @NotNull InputStream getDataInputStream() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void dispose() {
		IOUtils.closeQuietly(inputStream);
	}
}
