package jadx.utils;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;

public class Utils {

	public static String cleanObjectName(String obj) {
		int last = obj.length() - 1;
		if (obj.charAt(0) == 'L' && obj.charAt(last) == ';')
			return obj.substring(1, last).replace('/', '.');
		else
			return obj;
	}

	public static String makeQualifiedObjectName(String obj) {
		return 'L' + obj.replace('.', '/') + ';';
	}

	public static String escape(String str) {
		return str.replace('.', '_').replace('/', '_').replace(';', '_')
				.replace('$', '_').replace("[]", "_A");
	}

	public static String listToString(Iterable<?> list) {
		if (list == null)
			return "";

		StringBuilder str = new StringBuilder();
		for (Iterator<?> it = list.iterator(); it.hasNext();) {
			Object o = it.next();
			str.append(o.toString());
			if (it.hasNext())
				str.append(", ");
		}
		return str.toString();
	}

	public static boolean deleteFolder(File dir) {
		File[] files = dir.listFiles();
		if (files != null) {
			for (File f : files) {
				if (f.isDirectory())
					deleteFolder(f);
				else
					f.delete();
			}
		}
		return dir.delete();
	}

	public static String getStackTrace(Throwable throwable) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw, true);
		throwable.printStackTrace(pw);
		return sw.getBuffer().toString();
	}

	public static String mergeSignature(List<String> list) {
		StringBuilder sb = new StringBuilder();
		for (String s : list) {
			sb.append(s);
		}
		return sb.toString();
	}
}
