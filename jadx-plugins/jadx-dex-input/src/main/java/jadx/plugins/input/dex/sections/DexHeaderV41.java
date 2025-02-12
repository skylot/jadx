package jadx.plugins.input.dex.sections;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import static jadx.plugins.input.dex.utils.DataReader.readU4;

public class DexHeaderV41 {

	public static @Nullable DexHeaderV41 readIfPresent(byte[] content) {
		int headerSize = readU4(content, 36);
		if (headerSize < 120) {
			return null;
		}
		int fileSize = readU4(content, 32);
		int containerSize = readU4(content, 112);
		int headerOffset = readU4(content, 116);
		return new DexHeaderV41(fileSize, containerSize, headerOffset);
	}

	public static List<Integer> readSubDexOffsets(byte[] content, DexHeaderV41 header) {
		int start = 0;
		int end = header.getFileSize();
		int limit = Math.min(header.getContainerSize(), content.length);
		List<Integer> list = new ArrayList<>();
		while (true) {
			list.add(start);
			start = end;
			if (start >= limit) {
				break;
			}
			int nextFileSize = readU4(content, start + 32);
			end = start + nextFileSize;
		}
		return list;
	}

	private final int fileSize;
	private final int containerSize;
	private final int headerOffset;

	public DexHeaderV41(int fileSize, int containerSize, int headerOffset) {
		this.fileSize = fileSize;
		this.containerSize = containerSize;
		this.headerOffset = headerOffset;
	}

	public int getFileSize() {
		return fileSize;
	}

	public int getContainerSize() {
		return containerSize;
	}

	public int getHeaderOffset() {
		return headerOffset;
	}
}
