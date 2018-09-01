package jadx.core.utils;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import jadx.api.JadxDecompiler;
import jadx.core.codegen.CodeWriter;

public class Utils {

	public static final String JADX_API_PACKAGE = JadxDecompiler.class.getPackage().getName();

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
		StringBuilder sb = new StringBuilder();
		listToString(sb, objects, joiner, Object::toString);
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

	public static String arrayToString(Object[] array) {
		if (array == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < array.length; i++) {
			if (i != 0) {
				sb.append(", ");
			}
			sb.append(array[i]);
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
		int cutIndex = -1;
		int length = stackTrace.length;
		for (int i = 0; i < length; i++) {
			StackTraceElement stackTraceElement = stackTrace[i];
			if (stackTraceElement.getClassName().startsWith(JADX_API_PACKAGE)) {
				cutIndex = i;
			} else if (cutIndex > 0) {
				cutIndex = i;
				break;
			}
		}
		if (cutIndex > 0 && cutIndex < length) {
			th.setStackTrace(Arrays.copyOfRange(stackTrace, 0, cutIndex));
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> lockList(List<T> list) {
		if (list.isEmpty()) {
			return Collections.emptyList();
		}
		if (list.size() == 1) {
			return Collections.singletonList(list.get(0));
		}
		return new ImmutableList<>(list);
	}
}
