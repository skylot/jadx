package jadx.api.plugins.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

public class Utils {

	public static <T> void addToList(Collection<T> list, @Nullable T item) {
		if (item != null) {
			list.add(item);
		}
	}

	public static <T, I> void addToList(Collection<T> list, @Nullable I item, Function<I, T> map) {
		if (item != null) {
			T value = map.apply(item);
			if (value != null) {
				list.add(value);
			}
		}
	}

	public static <T> List<T> concat(List<T> a, List<T> b) {
		int aSize = a.size();
		int bSize = b.size();
		if (aSize == 0 && bSize == 0) {
			return Collections.emptyList();
		}
		if (aSize == 0) {
			return b;
		}
		if (bSize == 0) {
			return a;
		}
		List<T> list = new ArrayList<>(aSize + bSize);
		list.addAll(a);
		list.addAll(b);
		return list;
	}

	public static <T> List<T> concatDistinct(List<T> a, List<T> b) {
		int aSize = a.size();
		int bSize = b.size();
		if (aSize == 0 && bSize == 0) {
			return Collections.emptyList();
		}
		if (aSize == 0) {
			return b;
		}
		if (bSize == 0) {
			return a;
		}
		Set<T> set = new LinkedHashSet<>(aSize + bSize);
		set.addAll(a);
		set.addAll(b);
		return new ArrayList<>(set);
	}

	public static <T> String listToStr(List<T> list) {
		if (list == null) {
			return "null";
		}
		if (list.isEmpty()) {
			return "";
		}
		if (list.size() == 1) {
			return Objects.toString(list.get(0));
		}
		StringBuilder sb = new StringBuilder();
		Iterator<T> it = list.iterator();
		sb.append(it.next());
		while (it.hasNext()) {
			sb.append(", ").append(it.next());
		}
		return sb.toString();
	}

	public static String formatOffset(int offset) {
		return String.format("0x%04x", offset);
	}

	@SafeVarargs
	public static <T> Set<T> constSet(T... arr) {
		return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(arr)));
	}
}
