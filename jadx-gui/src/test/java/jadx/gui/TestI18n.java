package jadx.gui;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestI18n {

	private List<String> reference;
	private String referenceName;

	@Test
	public void filesExactlyMatch() throws IOException {
		Path path = Paths.get("./src/main/resources/i18n");
		assertTrue(Files.exists(path));
		Files.list(path).forEach(p -> {
			List<String> lines;
			try {
				lines = Files.readAllLines(p);
				if (reference == null) {
					reference = lines;
					referenceName = p.getFileName().toString();
				}
				else {
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
					fail(path, i + 1);
				}
			}
		}
		if (lines.size() != reference.size()) {
			fail(path, reference.size());
		}
	}

	private static String trimComment(String string) {
		return string.startsWith("#") ? string.substring(1) : string;
	}

	private void fail(Path path, int line) {
		Assertions.fail("I18n files " + path.getFileName() + " and " + referenceName + " differ in line " + line);
	}
}
