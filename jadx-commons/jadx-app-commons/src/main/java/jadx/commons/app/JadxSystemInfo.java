package jadx.commons.app;

import java.util.Locale;

public class JadxSystemInfo {
	public static final String JAVA_VM = System.getProperty("java.vm.name", "?");
	public static final String JAVA_VER = System.getProperty("java.version", "?");

	public static final String OS_NAME = System.getProperty("os.name", "?");
	public static final String OS_ARCH = System.getProperty("os.arch", "?");
	public static final String OS_VERSION = System.getProperty("os.version", "?");

	private static final String OS_NAME_LOWER = OS_NAME.toLowerCase(Locale.ENGLISH);
	public static final boolean IS_WINDOWS = OS_NAME_LOWER.startsWith("windows");
	public static final boolean IS_MAC = OS_NAME_LOWER.startsWith("mac");
	public static final boolean IS_LINUX = !IS_WINDOWS && !IS_MAC;
	public static final boolean IS_UNIX = !IS_WINDOWS;

	private static final String OS_ARCH_LOWER = OS_NAME.toLowerCase(Locale.ENGLISH);
	public static final boolean IS_AMD64 = OS_ARCH_LOWER.equals("amd64");
	public static final boolean IS_ARM64 = OS_ARCH_LOWER.equals("aarch64");

	private JadxSystemInfo() {
	}
}
