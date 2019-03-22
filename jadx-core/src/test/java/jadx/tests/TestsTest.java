package jadx.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

public class TestsTest {

	@Test
	public void noJUnit4Asssertions() throws IOException {
		noJUnit4Asssertions(".");
		noJUnit4Asssertions("../jadx-cli");
		noJUnit4Asssertions("../jadx-gui");
	}

	private void noJUnit4Asssertions(String path) throws IOException {
		Path dir = Paths.get(path, "src/test/java");
		assertTrue(Files.exists(dir));
		Files.walk(dir)
				.filter(p -> p.getFileName().toString().endsWith(".java")
						&& !p.getFileName().toString().endsWith(TestsTest.class.getSimpleName() + ".java"))
				.forEach(p -> {
					try {

						List<String> lines = Files.readAllLines(p);

						for (String line : lines) {
							if (line.contains("org.junit.Assert")) {
								String className = dir.relativize(p).toString();
								className = className.substring(0, className.length() - ".java".length());
								className = className.replace(File.separatorChar, '.');

								fail("Test class " + className + " should be migrated to JUnit 5");
							}
						}
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				});
	}
}
