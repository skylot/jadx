package jadx.gui.utils.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import static jadx.gui.utils.Utils.caseChar;

public class StringRef implements CharSequence {

	private final String refStr;
	private final int offset;
	private final int length;

	private int hash;

	public static StringRef subString(String str, int from, int to) {
		return new StringRef(str, from, to - from);
	}

	public static StringRef subString(String str, int from) {
		return subString(str, from, str.length());
	}

	public static StringRef fromStr(String str) {
		return new StringRef(str, 0, str.length());
	}

	private StringRef(String str, int from, int length) {
		this.refStr = str;
		this.offset = from;
		this.length = length;
	}

	@Override
	public int length() {
		return length;
	}

	@Override
	public char charAt(int index) {
		return refStr.charAt(offset + index);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return subString(refStr, start, end);
	}

	public StringRef trim() {
		int start = offset;
		int end = start + length;
		String str = refStr;

		while ((start < end) && (str.charAt(start) <= ' ')) {
			start++;
		}
		while ((start < end) && (str.charAt(end - 1) <= ' ')) {
			end--;
		}
		if ((start > offset) || (end < offset + length)) {
			return subString(str, start, end);
		}
		return this;
	}

	public int indexOf(String str) {
		return indexOf(str, 0);
	}

	public int indexOf(String str, boolean caseInsensitive) {
		return indexOf(str, 0, caseInsensitive);
	}

	public int indexOf(String str, int from, boolean caseInsensitive) {
		return indexOf(refStr, offset, length, str, 0, str.length(), from, caseInsensitive);
	}

	public int indexOf(String str, int from) {
		return indexOf(refStr, offset, length, str, 0, str.length(), from, false);
	}

	private static int indexOf(String source, int sourceOffset, int sourceCount,
	                           String target, int targetOffset, int targetCount,
	                           int fromIndex, boolean caseInsensitive) {
		if (fromIndex >= sourceCount) {
			return (targetCount == 0 ? sourceCount : -1);
		}
		if (fromIndex < 0) {
			fromIndex = 0;
		}
		if (targetCount == 0) {
			return -1;
		}
		char first = caseChar(target.charAt(targetOffset), caseInsensitive);
		int max = sourceOffset + (sourceCount - targetCount);
		for (int i = sourceOffset + fromIndex; i <= max; i++) {
			if (caseChar(source.charAt(i), caseInsensitive) != first) {
				while (++i <= max && caseChar(source.charAt(i), caseInsensitive) != first) {
				}
			}
			if (i <= max) {
				int j = i + 1;
				int end = j + targetCount - 1;
				int k = targetOffset + 1;
				while (j < end && caseChar(source.charAt(j), caseInsensitive) == caseChar(target.charAt(k), caseInsensitive)) {
					j++;
					k++;
				}
				if (j == end) {
					return i - sourceOffset;
				}
			}
		}
		return -1;
	}

	public static List<StringRef> split(String str, String splitBy) {
		int len = str.length();
		int targetLen = splitBy.length();
		if (len == 0 || targetLen == 0) {
			return Collections.emptyList();
		}
		int pos = -targetLen;
		List<StringRef> list = new ArrayList<>();
		while (true) {
			int start = pos + targetLen;
			pos = indexOf(str, 0, len, splitBy, 0, targetLen, start, false);
			if (pos == -1) {
				if (start != len) {
					list.add(subString(str, start, len));
				}
				break;
			} else {
				list.add(subString(str, start, pos));
			}
		}
		return list;
	}

	public int hashCode() {
		int h = hash;
		int len = length;
		if (h == 0 && len > 0) {
			int off = offset;
			String str = this.refStr;
			for (int i = 0; i < len; i++) {
				h = 31 * h + str.charAt(off++);
			}
			hash = h;
		}
		return h;
	}

	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof StringRef)) {
			return false;
		}
		StringRef otherSlice = (StringRef) other;
		int len = this.length;
		if (len != otherSlice.length) {
			return false;
		}
		int i = offset;
		int j = otherSlice.offset;
		String refStr = this.refStr;
		String otherRefStr = otherSlice.refStr;
		while (len-- != 0) {
			if (refStr.charAt(i++) != otherRefStr.charAt(j++)) {
				return false;
			}
		}
		return true;
	}

	@NotNull
	@Override
	public String toString() {
		int len = this.length;
		if (len == 0) {
			return "";
		}
		int offset = this.offset;
		return refStr.substring(offset, offset + len);
	}
}
