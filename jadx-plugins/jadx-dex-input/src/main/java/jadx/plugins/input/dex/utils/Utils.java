package jadx.plugins.input.dex.utils;

import java.util.Iterator;
import java.util.List;

public class Utils {

	public static <T> String listToStr(List<T> collection) {
		if (collection == null) {
			return "null";
		}
		if (collection.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		Iterator<T> it = collection.iterator();
		if (it.hasNext()) {
			sb.append(it.next());
		}
		while (it.hasNext()) {
			sb.append(", ").append(it.next());
		}
		return sb.toString();
	}

	public static String formatOffset(int offset) {
		return String.format("0x%04x", offset);
	}
}
