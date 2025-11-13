package jadx.core.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiPredicate;
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

	public static <T, U> boolean orderedEquals(List<T> list1, List<U> list2, BiPredicate<T, U> comparer) {
		if (list1 == list2) {
			return true;
		}
		if (list1.size() != list2.size()) {
			return false;
		}
		final Iterator<T> iter1 = list1.iterator();
		final Iterator<U> iter2 = list2.iterator();
		while (iter1.hasNext() && iter2.hasNext()) {
			final T item1 = iter1.next();
			final U item2 = iter2.next();
			if (!comparer.test(item1, item2)) {
				return false;
			}
		}
		return !iter1.hasNext() && !iter2.hasNext();
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

	public static <T> @Nullable T last(List<T> list) {
		if (list == null || list.isEmpty()) {
			return null;
		}
		return list.get(list.size() - 1);
	}

	public static <T> @Nullable T removeLast(List<T> list) {
		int size = list.size();
		if (size == 0) {
			return null;
		}
		return list.remove(size - 1);
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

	public static <T> List<T> concat(T first, T[] values) {
		List<T> list = new ArrayList<>(1 + values.length);
		list.add(first);
		list.addAll(Arrays.asList(values));
		return list;
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

	public static <T> List<T> enumerationToList(Enumeration<T> enumeration) {
		if (enumeration == null || enumeration == Collections.emptyEnumeration()) {
			return Collections.emptyList();
		}
		List<T> list = new ArrayList<>();
		while (enumeration.hasMoreElements()) {
			list.add(enumeration.nextElement());
		}
		return list;
	}
}
