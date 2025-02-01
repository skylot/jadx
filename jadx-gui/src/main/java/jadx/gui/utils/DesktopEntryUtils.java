package jadx.gui.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.export.TemplateFile;
import jadx.core.utils.files.FileUtils;

public class DesktopEntryUtils {
	private static final Logger LOG = LoggerFactory.getLogger(DesktopEntryUtils.class);
	private static final Map<Integer, String> SIZE_TO_LOGO_MAP = Map.of(
			16, "jadx-logo-16px.png",
			32, "jadx-logo-32px.png",
			48, "jadx-logo-48px.png",
			252, "jadx-logo.png",
			256, "jadx-logo.png");
	private static final Path XDG_DESKTOP_MENU_COMMAND_PATH = findExecutablePath("xdg-desktop-menu");
	private static final Path XDG_ICON_RESOURCE_COMMAND_PATH = findExecutablePath("xdg-icon-resource");

	public static boolean createDesktopEntry() {
		if (XDG_DESKTOP_MENU_COMMAND_PATH == null) {
			LOG.error("xdg-desktop-menu was not found in $PATH");
			return false;
		}
		if (XDG_ICON_RESOURCE_COMMAND_PATH == null) {
			LOG.error("xdg-icon-resource was not found in $PATH");
			return false;
		}
		Path desktopTempFile = FileUtils.createTempFileNonPrefixed("jadx-gui.desktop");
		Path iconTempFolder = FileUtils.createTempDir("logos");
		LOG.debug("Creating desktop with temp files: {}, {}", desktopTempFile, iconTempFolder);
		try {
			return createDesktopEntry(desktopTempFile, iconTempFolder);
		} finally {
			try {
				FileUtils.deleteFileIfExists(desktopTempFile);
				FileUtils.deleteDirIfExists(iconTempFolder);
			} catch (IOException e) {
				LOG.error("Failed to clean up temp files", e);
			}
		}
	}

	private static boolean createDesktopEntry(Path desktopTempFile, Path iconTempFolder) {
		String launchScriptPath = getLaunchScriptPath();
		if (launchScriptPath == null) {
			return false;
		}
		for (Map.Entry<Integer, String> entry : SIZE_TO_LOGO_MAP.entrySet()) {
			Path path = iconTempFolder.resolve(entry.getKey() + ".png");
			if (!writeLogoFile(entry.getValue(), path)) {
				return false;
			}
			if (!installIcon(entry.getKey(), path)) {
				return false;
			}
		}
		if (!writeDesktopFile(launchScriptPath, desktopTempFile)) {
			return false;
		}
		return installDesktopEntry(desktopTempFile);
	}

	private static boolean installDesktopEntry(Path desktopTempFile) {
		try {
			ProcessBuilder desktopFileInstallCommand = new ProcessBuilder(Objects.requireNonNull(XDG_DESKTOP_MENU_COMMAND_PATH).toString(),
					"install", desktopTempFile.toString());
			Process process = desktopFileInstallCommand.start();
			int statusCode = process.waitFor();
			if (statusCode != 0) {
				LOG.error("Got error code {} while installing desktop file", statusCode);
				return false;
			}
		} catch (Exception e) {
			LOG.error("Failed to install desktop file", e);
			return false;
		}
		LOG.info("Successfully installed desktop file");
		return true;
	}

	private static boolean installIcon(int size, Path iconPath) {
		try {
			ProcessBuilder iconInstallCommand =
					new ProcessBuilder(Objects.requireNonNull(XDG_ICON_RESOURCE_COMMAND_PATH).toString(), "install", "--novendor", "--size",
							String.valueOf(size), iconPath.toString(),
							"jadx");
			Process process = iconInstallCommand.start();
			int statusCode = process.waitFor();
			if (statusCode != 0) {
				LOG.error("Got error code {} while installing icon of size {}", statusCode, size);
				return false;
			}
		} catch (Exception e) {
			LOG.error("Failed to install icon of size {}", size, e);
			return false;
		}
		LOG.info("Successfully installed icon of size {}", size);
		return true;
	}

	private static Path findExecutablePath(String executableName) {
		for (String pathDirectory : System.getenv("PATH").split(File.pathSeparator)) {
			Path path = Paths.get(pathDirectory, executableName);
			if (path.toFile().isFile() && path.toFile().canExecute()) {
				return path;
			}
		}
		return null;
	}

	private static boolean writeDesktopFile(String launchScriptPath, Path desktopFilePath) {
		try {
			TemplateFile tmpl = TemplateFile.fromResources("/files/jadx-gui.desktop.tmpl");
			tmpl.add("launchScriptPath", launchScriptPath);
			FileUtils.writeFile(desktopFilePath, tmpl.build());
		} catch (Exception e) {
			LOG.error("Failed to save .desktop file at: {}", desktopFilePath, e);
			return false;
		}
		LOG.debug("Wrote .desktop file to {}", desktopFilePath);
		return true;
	}

	private static boolean writeLogoFile(String logoFile, Path logoPath) {
		try (InputStream is = DesktopEntryUtils.class.getResourceAsStream("/logos/" + logoFile)) {
			FileUtils.writeFile(logoPath, is);
		} catch (Exception e) {
			LOG.error("Failed to write logo file at: {}", logoPath, e);
			return false;
		}
		LOG.debug("Wrote logo file to: {}", logoPath);
		return true;
	}

	public static @Nullable String getLaunchScriptPath() {
		String launchScriptPath = System.getProperty("jadx.launchScript.path");
		if (launchScriptPath.isEmpty()) {
			LOG.error(
					"The jadx.launchScript.path property is not set. Please launch JADX with the bundled launch script or set it to the appropriate value yourself.");
			return null;
		}
		LOG.debug("JADX launch script path: {}", launchScriptPath);
		return launchScriptPath;
	}
}
