package jadx.gui;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class TestI18n {

	private static Path guiJavaPath;
	private static Path i18nPath;

	private List<String> reference;
	private String referenceName;

	@BeforeAll
	public static void init() {
		i18nPath = Paths.get("src/main/resources/i18n");
		assertTrue(Files.exists(i18nPath));
		guiJavaPath = Paths.get("src/main/java");
		assertTrue(Files.exists(guiJavaPath));
	}

	@Test
	public void filesExactlyMatch() throws IOException {
		Files.list(i18nPath).forEach(p -> {
			List<String> lines;
			try {
				lines = Files.readAllLines(p);
				if (reference == null) {
					reference = lines;
					referenceName = p.getFileName().toString();
				} else {
					compareToReference(p);
				}
			} catch (IOException e) {
				Assertions.fail("Error " + e.getMessage());
			}
		});
	}

	private void compareToReference(Path path) throws IOException {
		List<String> lines = Files.readAllLines(path);
		for (int i = 0; i < reference.size(); i++) {
			String line = trimComment(reference.get(i));
			int p0 = line.indexOf('=');
			if (p0 != -1) {
				String prefix = line.substring(0, p0 + 1);
				if (i >= lines.size() || !trimComment(lines.get(i)).startsWith(prefix)) {
					failLine(path, i + 1);
				}
			}
		}
		if (lines.size() != reference.size()) {
			failLine(path, reference.size());
		}
	}

	private static String trimComment(String string) {
		return string.startsWith("#") ? string.substring(1) : string;
	}

	private void failLine(Path path, int line) {
		fail("I18n files " + path.getFileName() + " and " + referenceName + " differ in line " + line);
	}

	@Test
	public void keyIsUsed() throws IOException {
		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(i18nPath.resolve("Messages_en_US.properties"))) {
			properties.load(reader);
		}

		Set<String> keys = new HashSet<>();
		for (Object key : properties.keySet()) {
			keys.add("\"" + key + '"');
		}

		Files.walk(guiJavaPath).filter(p -> Files.isRegularFile(p)).forEach(p -> {
			try {
				List<String> lines = Files.readAllLines(p);
				for (String line : lines) {
					for (Iterator<String> it = keys.iterator(); it.hasNext();) {
						if (line.contains(it.next())) {
							it.remove();
						}
					}
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});

		assertThat("keys not used", keys, empty());
	}
}
