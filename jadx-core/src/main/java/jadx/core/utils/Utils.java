package jadx.core.utils;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import jadx.api.ICodeWriter;
import jadx.api.JadxDecompiler;
import jadx.core.dex.visitors.DepthTraversal;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class Utils {

	private static final String JADX_API_PACKAGE = JadxDecompiler.class.getPackage().getName();
	private static final String STACKTRACE_STOP_CLS_NAME = DepthTraversal.class.getName();

	private Utils() {
	}

	public static String cleanObjectName(String obj) {
		if (obj.charAt(0) == 'L') {
			int last = obj.length() - 1;
			if (obj.charAt(last) == ';') {
				return obj.substring(1, last).replace('/', '.');
			}
		}
		return obj;
	}

	public static String cutObject(String obj) {
		if (obj.charAt(0) == 'L') {
			return obj.substring(1, obj.length() - 1);
		}
		return obj;
	}

	public static String makeQualifiedObjectName(String obj) {
		return 'L' + obj.replace('.', '/') + ';';
	}

	public static String strRepeat(String str, int count) {
		StringBuilder sb = new StringBuilder(str.length() * count);
		for (int i = 0; i < count; i++) {
			sb.append(str);
		}
		return sb.toString();
	}

	public static String listToString(Iterable<?> objects) {
		return listToString(objects, ", ");
	}

	public static String listToString(Iterable<?> objects, String joiner) {
		if (objects == null) {
			return "";
		}
		return listToString(objects, joiner, Objects::toString);
	}

	public static <T> String listToString(Iterable<T> objects, Function<T, String> toStr) {
		return listToString(objects, ", ", toStr);
	}

	public static <T> String listToString(Iterable<T> objects, String joiner, Function<T, String> toStr) {
		StringBuilder sb = new StringBuilder();
		listToString(sb, objects, joiner, toStr);
		return sb.toString();
	}

	public static <T> void listToString(StringBuilder sb, Iterable<T> objects, String joiner) {
		listToString(sb, objects, joiner, Objects::toString);
	}

	public static <T> void listToString(StringBuilder sb, Iterable<T> objects, String joiner, Function<T, String> toStr) {
		if (objects == null) {
			return;
		}
		Iterator<T> it = objects.iterator();
		if (it.hasNext()) {
			sb.append(toStr.apply(it.next()));
		}
		while (it.hasNext()) {
			sb.append(joiner).append(toStr.apply(it.next()));
		}
	}

	public static <T> String arrayToStr(T[] arr) {
		int len = arr == null ? 0 : arr.length;
		if (len == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(arr[0]);
		for (int i = 1; i < len; i++) {
			sb.append(", ").append(arr[i]);
		}
		return sb.toString();
	}

	public static String concatStrings(List<String> list) {
		if (isEmpty(list)) {
			return "";
		}
		if (list.size() == 1) {
			return list.get(0);
		}
		StringBuilder sb = new StringBuilder();
		list.forEach(sb::append);
		return sb.toString();
	}

	public static String currentStackTrace() {
		return getStackTrace(new Exception());
	}

	public static String currentStackTrace(int skipFrames) {
		Exception e = new Exception();
		StackTraceElement[] stackTrace = e.getStackTrace();
		int len = stackTrace.length;
		if (skipFrames < len) {
			e.setStackTrace(Arrays.copyOfRange(stackTrace, skipFrames, len));
		}
		return getStackTrace(e);
	}

	public static String getFullStackTrace(Throwable throwable) {
		return getStackTrace(throwable, false);
	}

	public static String getStackTrace(Throwable throwable) {
		return getStackTrace(throwable, true);
	}

	private static String getStackTrace(Throwable throwable, boolean filter) {
		if (throwable == null) {
			return "";
		}
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw, true);
		if (filter) {
			filterRecursive(throwable);
		}
		throwable.printStackTrace(pw);
		return sw.getBuffer().toString();
	}

	public static void appendStackTrace(ICodeWriter code, Throwable throwable) {
		if (throwable == null) {
			return;
		}
		code.startLine();
		OutputStream w = new OutputStream() {
			@Override
			public void write(int b) {
				char c = (char) b;
				switch (c) {
					case '\n':
						code.startLine();
						break;

					case '\r':
						// ignore
						break;

					default:
						code.add(c);
						break;
				}
			}
		};
		try (PrintWriter pw = new PrintWriter(w, true)) {
			filterRecursive(throwable);
			throwable.printStackTrace(pw);
			pw.flush();
		}
	}

	private static void filterRecursive(Throwable th) {
		try {
			filter(th);
		} catch (Exception e) {
			// ignore filter exceptions
		}
		Throwable cause = th.getCause();
		if (cause != null) {
			filterRecursive(cause);
		}
	}

	private static void filter(Throwable th) {
		StackTraceElement[] stackTrace = th.getStackTrace();
		int length = stackTrace.length;
		StackTraceElement prevElement = null;
		for (int i = 0; i < length; i++) {
			StackTraceElement stackTraceElement = stackTrace[i];
			String clsName = stackTraceElement.getClassName();
			if (clsName.equals(STACKTRACE_STOP_CLS_NAME)
					|| clsName.startsWith(JADX_API_PACKAGE)
					|| Objects.equals(prevElement, stackTraceElement)) {
				th.setStackTrace(Arrays.copyOfRange(stackTrace, 0, i));
				return;
			}
			prevElement = stackTraceElement;
		}
		// stop condition not found -> just cut tail to any jadx class
		for (int i = length - 1; i >= 0; i--) {
			String clsName = stackTrace[i].getClassName();
			if (clsName.startsWith("jadx.")) {
				if (clsName.startsWith("jadx.tests.")) {
					continue;
				}
				th.setStackTrace(Arrays.copyOfRange(stackTrace, 0, i));
				return;
			}
		}
	}

	public static <T, R> List<R> collectionMap(Collection<T> list, Function<T, R> mapFunc) {
		if (list == null || list.isEmpty()) {
			return Collections.emptyList();
		}
		List<R> result = new ArrayList<>(list.size());
		for (T t : list) {
			result.add(mapFunc.apply(t));
		}
		return result;
	}

	public static <T, R> List<R> collectionMapNoNull(Collection<T> list, Function<T, R> mapFunc) {
		if (list == null || list.isEmpty()) {
			return Collections.emptyList();
		}
		List<R> result = new ArrayList<>(list.size());
		for (T t : list) {
			R r = mapFunc.apply(t);
			if (r != null) {
				result.add(r);
			}
		}
		return result;
	}

	public static <T> boolean containsInListByRef(List<T> list, T element) {
		if (isEmpty(list)) {
			return false;
		}
		for (T t : list) {
			if (t == element) {
				return true;
			}
		}
		return false;
	}

	public static <T> int indexInListByRef(List<T> list, T element) {
		if (list == null || list.isEmpty()) {
			return -1;
		}
		int size = list.size();
		for (int i = 0; i < size; i++) {
			T t = list.get(i);
			if (t == element) {
				return i;
			}
		}
		return -1;
	}

	public static <T> List<T> lockList(List<T> list) {
		if (list.isEmpty()) {
			return Collections.emptyList();
		}
		if (list.size() == 1) {
			return Collections.singletonList(list.get(0));
		}
		return new ImmutableList<>(list);
	}

	/**
	 * Sub list from startIndex (inclusive) to list end
	 */
	public static <T> List<T> listTail(List<T> list, int startIndex) {
		if (startIndex == 0) {
			return list;
		}
		int size = list.size();
		if (startIndex >= size) {
			return Collections.emptyList();
		}
		return list.subList(startIndex, size);
	}

	public static <T> List<T> mergeLists(List<T> first, List<T> second) {
		if (isEmpty(first)) {
			return second;
		}
		if (isEmpty(second)) {
			return first;
		}
		List<T> result = new ArrayList<>(first.size() + second.size());
		result.addAll(first);
		result.addAll(second);
		return result;
	}

	public static <T> Set<T> mergeSets(Set<T> first, Set<T> second) {
		if (isEmpty(first)) {
			return second;
		}
		if (isEmpty(second)) {
			return first;
		}
		Set<T> result = new HashSet<>(first.size() + second.size());
		result.addAll(first);
		result.addAll(second);
		return result;
	}

	public static Map<String, String> newConstStringMap(String... parameters) {
		int len = parameters.length;
		if (len == 0) {
			return Collections.emptyMap();
		}
		if (len % 2 != 0) {
			throw new IllegalArgumentException("Incorrect arguments count: " + len);
		}
		Map<String, String> result = new HashMap<>(len / 2);
		for (int i = 0; i < len - 1; i += 2) {
			result.put(parameters[i], parameters[i + 1]);
		}
		return Collections.unmodifiableMap(result);
	}

	/**
	 * Merge two maps. Return HashMap as result. Second map will override values from first map.
	 */
	public static <K, V> Map<K, V> mergeMaps(Map<K, V> first, Map<K, V> second) {
		if (isEmpty(first)) {
			return second;
		}
		if (isEmpty(second)) {
			return first;
		}
		Map<K, V> result = new HashMap<>(first.size() + second.size());
		result.putAll(first);
		result.putAll(second);
		return result;
	}

	/**
	 * Build map from list of values with value to key mapping function
	 * <br>
	 * Similar to:
	 * <br>
	 * {@code list.stream().collect(Collectors.toMap(mapKey, Function.identity())); }
	 */
	public static <K, V> Map<K, V> groupBy(List<V> list, Function<V, K> mapKey) {
		Map<K, V> map = new HashMap<>(list.size());
		for (V v : list) {
			map.put(mapKey.apply(v), v);
		}
		return map;
	}

	/**
	 * Simple DFS visit for tree (cycles not allowed)
	 */
	public static <T> void treeDfsVisit(T root, Function<T, List<T>> childrenProvider, Consumer<T> visitor) {
		multiRootTreeDfsVisit(Collections.singletonList(root), childrenProvider, visitor);
	}

	public static <T> void multiRootTreeDfsVisit(List<T> roots, Function<T, List<T>> childrenProvider, Consumer<T> visitor) {
		Deque<T> queue = new ArrayDeque<>(roots);
		while (true) {
			T current = queue.pollLast();
			if (current == null) {
				return;
			}
			visitor.accept(current);
			for (T child : childrenProvider.apply(current)) {
				queue.addLast(child);
			}
		}
	}

	@Nullable
	public static <T> T getOne(@Nullable List<T> list) {
		if (list == null || list.size() != 1) {
			return null;
		}
		return list.get(0);
	}

	@Nullable
	public static <T> T getOne(@Nullable Collection<T> collection) {
		if (collection == null || collection.size() != 1) {
			return null;
		}
		return collection.iterator().next();
	}

	@Nullable
	public static <T> T first(List<T> list) {
		if (list.isEmpty()) {
			return null;
		}
		return list.get(0);
	}

	@Nullable
	public static <T> T first(Iterable<T> list) {
		Iterator<T> it = list.iterator();
		if (!it.hasNext()) {
			return null;
		}
		return it.next();
	}

	@Nullable
	public static <T> T last(List<T> list) {
		if (list.isEmpty()) {
			return null;
		}
		return list.get(list.size() - 1);
	}

	@Nullable
	public static <T> T last(Iterable<T> list) {
		Iterator<T> it = list.iterator();
		if (!it.hasNext()) {
			return null;
		}
		while (true) {
			T next = it.next();
			if (!it.hasNext()) {
				return next;
			}
		}
	}

	public static <T> T getOrElse(@Nullable T obj, T defaultObj) {
		if (obj == null) {
			return defaultObj;
		}
		return obj;
	}

	public static <T> boolean isEmpty(Collection<T> col) {
		return col == null || col.isEmpty();
	}

	public static <T> boolean notEmpty(Collection<T> col) {
		return col != null && !col.isEmpty();
	}

	public static <K, V> boolean isEmpty(Map<K, V> map) {
		return map == null || map.isEmpty();
	}

	public static <T> boolean isEmpty(T[] arr) {
		return arr == null || arr.length == 0;
	}

	public static <T> boolean notEmpty(T[] arr) {
		return arr != null && arr.length != 0;
	}

	public static void checkThreadInterrupt() {
		if (Thread.currentThread().isInterrupted()) {
			throw new JadxRuntimeException("Thread interrupted");
		}
	}
}
