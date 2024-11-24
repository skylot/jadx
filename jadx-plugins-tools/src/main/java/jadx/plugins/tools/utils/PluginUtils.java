package jadx.plugins.tools.utils;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

public class PluginUtils {

	public static String removePrefix(String str, String prefix) {
		if (str.startsWith(prefix)) {
			return str.substring(prefix.length());
		}
		return str;
	}

	public static void downloadFile(String fileUrl, Path destPath) {
		try (InputStream in = URI.create(fileUrl).toURL().openStream()) {
			Files.copy(in, destPath, StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			throw new RuntimeException("Failed to download file: " + fileUrl, e);
		}
	}

	private static final Pattern VERSION_LONG = Pattern.compile(".*v?(\\d+\\.\\d+\\.\\d+).*");
	private static final Pattern VERSION_SHORT = Pattern.compile(".*v?(\\d+\\.\\d+).*");

	public static @Nullable String extractVersion(String str) {
		Matcher longMatcher = VERSION_LONG.matcher(str);
		if (longMatcher.matches()) {
			return longMatcher.group(1);
		}
		Matcher shortMatcher = VERSION_SHORT.matcher(str);
		if (shortMatcher.matches()) {
			return shortMatcher.group(1);
		}
		return null;
	}
}
