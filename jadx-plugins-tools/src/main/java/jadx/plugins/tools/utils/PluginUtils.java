package jadx.plugins.tools.utils;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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
}
