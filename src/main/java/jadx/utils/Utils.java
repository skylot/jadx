package jadx.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
	private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

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
		return str.replace('.', '_')
				.replace('/', '_')
				.replace(';', '_')
				.replace('$', '_')
				.replace('<', '_')
				.replace('>', '_')
				.replace("[]", "_A");
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

	public static String arrayToString(Object[] array) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < array.length; i++) {
			if (i != 0)
				sb.append(", ");
			sb.append(array[i]);
		}
		return sb.toString();
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

	public static String getJadxVersion() {
		try {
			Enumeration<URL> resources =
					new Utils().getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
			while (resources.hasMoreElements()) {
				Manifest manifest = new Manifest(resources.nextElement().openStream());
				String ver = manifest.getMainAttributes().getValue("jadx-version");
				if (ver != null)
					return ver;
			}
		} catch (IOException e) {
			LOG.error("Can't get manifest file", e);
		}
		return "dev";
	}
}
