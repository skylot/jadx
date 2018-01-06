package jadx.core.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Iterator;

import jadx.api.JadxDecompiler;

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

	public static String listToString(Iterable<?> list) {
		if (list == null) {
			return "";
		}
		StringBuilder str = new StringBuilder();
		for (Iterator<?> it = list.iterator(); it.hasNext(); ) {
			Object o = it.next();
			str.append(o);
			if (it.hasNext()) {
				str.append(", ");
			}
		}
		return str.toString();
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
}
