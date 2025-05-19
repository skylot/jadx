package jadx.gui.utils.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Automatically synchronizes i18n files with a reference (EN) file.
 * - Adds new lines from the reference file (commented out) to other language files.
 * - Removes lines from other language files that are not present in the reference file.
 * - Updates commented-out empty translations in other files with the reference text (commented).
 */
public class NLSAddNewLines {
	private static final Logger LOG = LoggerFactory.getLogger(NLSAddNewLines.class);

	private static final Path I18N_PATH = Paths.get("src/main/resources/i18n/");
	// Assumes this script is run from the project root, or jadx-gui is a direct subdirectory.
	// If jadx-gui is the project root, this might need adjustment or to be removed
	// if I18N_PATH is already relative to jadx-gui.
	private static final String GUI_MODULE_DIR_NAME = "jadx-gui";
	private static final Path GUI_MODULE_PREFIX_PATH = Paths.get(GUI_MODULE_DIR_NAME);

	public static void main(String[] args) {
		try {
			process();
		} catch (Exception e) {
			LOG.error("Failed to process i18n files", e);
		}
	}

	private static void process() throws Exception {
		String referenceFileName = "Messages_en_US.properties";
		Path refPath = getRefPath(referenceFileName);
		if (!Files.exists(refPath)) {
			LOG.error("Reference i18n file not found: {}", referenceFileName);
			return;
		}
		LOG.info("Using reference file: {}", refPath.toAbsolutePath());

		List<String> refFileLines = Files.readAllLines(refPath, StandardCharsets.UTF_8);

		Path i18nDir = refPath.toAbsolutePath().getParent();
		if (i18nDir == null) {
			LOG.error("Could not determine i18n directory from reference path: {}", refPath);
			return;
		}

		try (Stream<Path> pathStream = Files.list(i18nDir)) {
			pathStream
					.filter(path -> !path.getFileName().toString().equals(referenceFileName))
					.filter(path -> path.getFileName().toString().startsWith("Messages_") && path.toString().endsWith(".properties"))
					.forEach(targetPath -> {
						try {
							LOG.info("Processing target file: {}", targetPath.toAbsolutePath());
							applySync(refFileLines, targetPath);
						} catch (Exception e) {
							LOG.error("Failed to sync file: {}", targetPath, e);
						}
					});
		}
		LOG.info("I18N synchronization process finished.");
	}

	/**
	 * Parses a list of lines from a properties file into a map of key-value pairs.
	 * Preserves the full line as value. Comments and empty lines will have null keys.
	 */
	private static Map<String, String> parseProperties(List<String> lines) {
		Map<String, String> properties = new LinkedHashMap<>();
		for (String line : lines) {
			String key = extractKey(line);
			// If key is null, it's a comment or blank line. We might not need these in the map
			// if we iterate over the original refFileLines later.
			// For simplicity here, we only store actual properties.
			if (key != null) {
				properties.put(key, line);
			}
		}
		return properties;
	}

	/**
	 * Extracts the key from a properties file line.
	 * Returns null if the line is a comment, empty, or doesn't seem to be a key-value pair.
	 */
	private static String extractKey(String line) {
		String trimmedLine = line.trim();
		if (trimmedLine.isEmpty() || trimmedLine.startsWith("#") || trimmedLine.startsWith("!")) {
			return null; // Comment or empty line
		}
		int separatorIndex = -1;
		for (int i = 0; i < trimmedLine.length(); i++) {
			char c = trimmedLine.charAt(i);
			if (c == '=' || c == ':') {
				separatorIndex = i;
				break;
			}
		}

		if (separatorIndex == -1) {
			// Line without a separator, could be a key with no value (less common in i18n)
			// or just a malformed line. For now, let's consider it a key if it's not empty.
			return trimmedLine;
		}

		return trimmedLine.substring(0, separatorIndex).trim();
	}

	private static void applySync(List<String> refFileLines, Path targetPath) throws IOException {
		List<String> originalTargetLines = Files.readAllLines(targetPath, StandardCharsets.UTF_8);
		Map<String, String> targetProperties = parseProperties(originalTargetLines);
		List<String> newTargetLines = new ArrayList<>();
		boolean updated = false;

		for (String refLine : refFileLines) {
			String refKey = extractKey(refLine);

			if (refKey == null) { // It's a comment or blank line from reference
				newTargetLines.add(refLine);
			} else {
				// It's a property line from reference
				if (targetProperties.containsKey(refKey)) {
					String targetLine = targetProperties.get(refKey);
					// Original logic: if target line is like "#key=" (commented, no value)
					// then use the commented reference value.
					if (targetLine.trim().startsWith("#")
							&& targetLine.trim().substring(1).trim().startsWith(refKey) // ensure it's the same key
							&& targetLine.trim().endsWith("=")) {
						newTargetLines.add("#" + refLine.trim()); // Use reference line, commented
					} else {
						newTargetLines.add(targetLine); // Use existing target line
					}
				} else {
					// Key from reference is missing in target, add it commented out
					newTargetLines.add("#" + refLine.trim());
				}
			}
		}

		// Check if files are different
		if (originalTargetLines.size() != newTargetLines.size()) {
			updated = true;
		} else {
			for (int i = 0; i < originalTargetLines.size(); i++) {
				if (!originalTargetLines.get(i).equals(newTargetLines.get(i))) {
					updated = true;
					break;
				}
			}
		}

		if (updated) {
			LOG.info("Updating {} ({} lines -> {} lines)", targetPath.getFileName(), originalTargetLines.size(), newTargetLines.size());
			Files.write(targetPath, newTargetLines, StandardCharsets.UTF_8);
		} else {
			LOG.info("No changes needed for {}", targetPath.getFileName());
		}
	}

	private static Path getRefPath(String referenceFileName) {
		// Path relative to project root (where src/main/resources exists)
		Path projectRootRelative = I18N_PATH.resolve(referenceFileName);
		if (Files.exists(projectRootRelative)) {
			return projectRootRelative.toAbsolutePath();
		}

		// Path relative to a module (e.g. jadx-gui/src/main/resources)
		// This assumes the script is run from one level above GUI_MODULE_DIR_NAME
		Path moduleRelative = GUI_MODULE_PREFIX_PATH.resolve(I18N_PATH).resolve(referenceFileName);
		if (Files.exists(moduleRelative)) {
			return moduleRelative.toAbsolutePath();
		}

		// Path if script is run from within the GUI_MODULE_DIR_NAME itself
		Path currentDirRelative = Paths.get(".").resolve(I18N_PATH).resolve(referenceFileName);
		if (Files.exists(currentDirRelative)) {
			return currentDirRelative.toAbsolutePath();
		}
		throw new RuntimeException("Can't find reference I18N: " + referenceFileName);
	}
}
