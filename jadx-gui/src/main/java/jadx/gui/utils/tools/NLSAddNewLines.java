package jadx.gui.utils.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Automatically add new i18n lines from reference (EN) into others languages
 */
public class NLSAddNewLines {
	private static final Logger LOG = LoggerFactory.getLogger(NLSAddNewLines.class);

	private static final Path I18N_PATH = Paths.get("src/main/resources/i18n/");
	private static final String GUI_MODULE_DIR = "jadx-gui";

	public static void main(String[] args) {
		try {
			process();
		} catch (Exception e) {
			LOG.error("Failed to add new i18n lines", e);
		}
	}

	private static void process() throws Exception {
		String reference = "Messages_en_US.properties";
		Path refPath = getRefPath(reference);
		List<String> refLines = Files.readAllLines(refPath);

		try (Stream<Path> pathStream = Files.list(refPath.toAbsolutePath().getParent())) {
			pathStream.forEach(path -> {
				if (path.getFileName().equals(refPath.getFileName())) {
					return;
				}
				try {
					applyFix(refLines, path);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}
	}

	private static void applyFix(List<String> refLines, Path path) throws IOException {
		List<String> lines = Files.readAllLines(path);
		int linesCount = lines.size();
		if (refLines.size() <= linesCount) {
			LOG.info("Skip {}, already fixed", path);
			return;
		}
		boolean updated = false;
		for (int i = 0; i < linesCount; i++) {
			String line = lines.get(i);
			String refLine = refLines.get(i);
			if (!isSameKey(refLine, line)) {
				if (refLine.isEmpty()) {
					lines.add(i, "");
				} else {
					lines.add(i, "#" + refLine);
				}
				updated = true;
			}
		}
		if (updated) {
			LOG.info("Updating {}", path);
			Files.write(path, lines, StandardCharsets.UTF_8);
		}
	}

	private static boolean isSameKey(String refLine, String line) {
		int refLen = refLine.length();
		int len = line.length();
		if (refLen == 0) {
			return len == 0;
		}
		if (len == 0) {
			return false;
		}
		int pos = 0;
		// skip comment and spaces
		while (pos < len) {
			char ch = line.charAt(pos);
			if (ch == '#' || ch == ' ') {
				pos++;
			} else {
				break;
			}
		}
		int refPos = 0;
		while (true) {
			char refCh = refLine.charAt(refPos);
			if (refCh == ' ' || refCh == '=') {
				return true;
			}
			char ch = line.charAt(pos);
			if (refCh != ch) {
				return false;
			}
			refPos++;
			pos++;
		}
	}

	private static Path getRefPath(String reference) {
		Path path = I18N_PATH.resolve(reference);
		if (Files.exists(path)) {
			return path;
		}
		Path rootPath = Paths.get(GUI_MODULE_DIR).resolve(I18N_PATH).resolve(reference);
		if (Files.exists(rootPath)) {
			return rootPath;
		}
		throw new RuntimeException("Can't find reference I18N: " + reference);
	}
}
