package jadx.gui.utils;

import java.util.Locale;

import jadx.api.JadxDecompiler;

public class SystemInfo {
	public static final String JADX_VERSION = JadxDecompiler.getVersion();

	public static final String JAVA_VM = System.getProperty("java.vm.name");
	public static final String JAVA_VER = System.getProperty("java.version");

	public static final String OS_NAME = System.getProperty("os.name");
	public static final String OS_VERSION = System.getProperty("os.version");

	private static final String LOWER_OS_NAME = OS_NAME.toLowerCase(Locale.ENGLISH);
	public static final boolean IS_WINDOWS = LOWER_OS_NAME.startsWith("windows");
	public static final boolean IS_MAC = LOWER_OS_NAME.startsWith("mac");
	public static final boolean IS_LINUX = LOWER_OS_NAME.startsWith("linux");

	private SystemInfo() {
	}
}
