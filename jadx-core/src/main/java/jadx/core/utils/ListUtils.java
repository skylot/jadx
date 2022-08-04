package jadx.core.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

public class ListUtils {

	public static <T> boolean isSingleElement(@Nullable List<T> list, T obj) {
		if (list == null || list.size() != 1) {
			return false;
		}
		return Objects.equals(list.get(0), obj);
	}

	public static <T> boolean unorderedEquals(List<T> first, List<T> second) {
		if (first.size() != second.size()) {
			return false;
		}
		return first.containsAll(second);
	}

	public static <T, R> List<R> map(Collection<T> list, Function<T, R> mapFunc) {
		if (list == null || list.isEmpty()) {
			return Collections.emptyList();
		}
		List<R> result = new ArrayList<>(list.size());
		for (T t : list) {
			result.add(mapFunc.apply(t));
		}
		return result;
	}

	public static <T> T first(List<T> list) {
		return list.get(0);
	}

	public static <T> T last(List<T> list) {
		return list.get(list.size() - 1);
	}

	public static <T extends Comparable<T>> List<T> distinctMergeSortedLists(List<T> first, List<T> second) {
		if (first.isEmpty()) {
			return second;
		}
		if (second.isEmpty()) {
			return first;
		}
		Set<T> set = new TreeSet<>(first);
		set.addAll(second);
		return new ArrayList<>(set);
	}

	public static <T> List<T> distinctList(List<T> list) {
		return new ArrayList<>(new LinkedHashSet<>(list));
	}

	/**
	 * Replace old element to new one.
	 * Support null and empty immutable list (created by Collections.emptyList())
	 */
	public static <T> List<T> safeReplace(List<T> list, T oldObj, T newObj) {
		if (list == null || list.isEmpty()) {
			// immutable empty list
			List<T> newList = new ArrayList<>(1);
			newList.add(newObj);
			return newList;
		}
		int idx = list.indexOf(oldObj);
		if (idx != -1) {
			list.set(idx, newObj);
		} else {
			list.add(newObj);
		}
		return list;
	}

	public static <T> void safeRemove(List<T> list, T obj) {
		if (list != null && !list.isEmpty()) {
			list.remove(obj);
		}
	}

	public static <T> List<T> safeRemoveAndTrim(List<T> list, T obj) {
		if (list == null || list.isEmpty()) {
			return list;
		}
		if (list.remove(obj)) {
			if (list.isEmpty()) {
				return Collections.emptyList();
			}
		}
		return list;
	}

	public static <T> List<T> safeAdd(List<T> list, T obj) {
		if (list == null || list.isEmpty()) {
			List<T> newList = new ArrayList<>(1);
			newList.add(obj);
			return newList;
		}
		list.add(obj);
		return list;
	}

	public static <T> List<T> filter(Collection<T> list, Predicate<T> filter) {
		if (list == null || list.isEmpty()) {
			return Collections.emptyList();
		}
		List<T> result = new ArrayList<>();
		for (T element : list) {
			if (filter.test(element)) {
				result.add(element);
			}
		}
		return result;
	}

	/**
	 * Search exactly one element in list by filter
	 *
	 * @return null if found not exactly one element (zero or more than one)
	 */
	@Nullable
	public static <T> T filterOnlyOne(List<T> list, Predicate<T> filter) {
		if (list == null || list.isEmpty()) {
			return null;
		}
		T found = null;
		for (T element : list) {
			if (filter.test(element)) {
				if (found != null) {
					// found second
					return null;
				}
				found = element;
			}
		}
		return found;
	}

	public static <T> boolean allMatch(Collection<T> list, Predicate<T> test) {
		if (list == null || list.isEmpty()) {
			return false;
		}
		for (T element : list) {
			if (!test.test(element)) {
				return false;
			}
		}
		return true;
	}

	public static <T> boolean noneMatch(Collection<T> list, Predicate<T> test) {
		return !anyMatch(list, test);
	}

	public static <T> boolean anyMatch(Collection<T> list, Predicate<T> test) {
		if (list == null || list.isEmpty()) {
			return false;
		}
		for (T element : list) {
			if (test.test(element)) {
				return true;
			}
		}
		return false;
	}
}
