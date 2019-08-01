package jadx.core.utils;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import jadx.api.JadxDecompiler;
import jadx.core.codegen.CodeWriter;
import jadx.core.dex.visitors.DepthTraversal;

public class Utils {

	private static final String JADX_API_PACKAGE = JadxDecompiler.class.getPackage().getName();
	private static final String STACKTRACE_STOP_CLS_NAME = DepthTraversal.class.getName();

	private Utils() {
	}

	public static String cleanObjectName(String obj) {
		int last = obj.length() - 1;
		if (obj.charAt(0) == 'L' && obj.charAt(last) == ';') {
			return obj.substring(1, last).replace('/', '.');
		}
		return obj;
	}

	public static String makeQualifiedObjectName(String obj) {
		return 'L' + obj.replace('.', '/') + ';';
	}

	public static String listToString(Iterable<?> objects) {
		return listToString(objects, ", ");
	}

	public static String listToString(Iterable<?> objects, String joiner) {
		if (objects == null) {
			return "";
		}
		return listToString(objects, joiner, Object::toString);
	}

	public static <T> String listToString(Iterable<T> objects, Function<T, String> toStr) {
		return listToString(objects, ", ", toStr);
	}

	public static <T> String listToString(Iterable<T> objects, String joiner, Function<T, String> toStr) {
		StringBuilder sb = new StringBuilder();
		listToString(sb, objects, joiner, toStr);
		return sb.toString();
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

	public static String getStackTrace(Throwable throwable) {
		if (throwable == null) {
			return "";
		}
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw, true);
		filterRecursive(throwable);
		throwable.printStackTrace(pw);
		return sw.getBuffer().toString();
	}

	public static void appendStackTrace(CodeWriter code, Throwable throwable) {
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
		for (int i = 0; i < length; i++) {
			StackTraceElement stackTraceElement = stackTrace[i];
			String clsName = stackTraceElement.getClassName();
			if (clsName.equals(STACKTRACE_STOP_CLS_NAME)
					|| clsName.startsWith(JADX_API_PACKAGE)) {
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

	public static <T> int indexInList(List<T> list, T element) {
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

	@Nullable
	public static <T> T last(List<T> list) {
		if (list.isEmpty()) {
			return null;
		}
		return list.get(list.size() - 1);
	}
}
