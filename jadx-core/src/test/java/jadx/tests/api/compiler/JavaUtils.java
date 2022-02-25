package jadx.tests.api.compiler;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaUtils {

	private static final Logger LOG = LoggerFactory.getLogger(JavaUtils.class);

	public static final int JAVA_VERSION_INT = getJavaVersionInt();

	public static boolean checkJavaVersion(int requiredVersion) {
		return JAVA_VERSION_INT >= requiredVersion;
	}

	private static int getJavaVersionInt() {
		String javaSpecVerStr = SystemUtils.JAVA_SPECIFICATION_VERSION;
		if (javaSpecVerStr == null) {
			LOG.warn("Unknown current java specification version, use 8 as fallback");
			return 8; // fallback version
		}
		if (javaSpecVerStr.startsWith("1.")) {
			return Integer.parseInt(javaSpecVerStr.substring(2));
		}
		return Integer.parseInt(javaSpecVerStr);
	}
}
