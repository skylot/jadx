package jadx.gui;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jadx.gui.utils.LangLocale;
import jadx.gui.utils.NLS;

import static java.nio.file.Paths.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class TestI18n {
	private static final String DEFAULT_LANG_FILE = "Messages_en_US.properties";

	private static Path i18nPath;
	private static Path refPath;
	private static Path guiJavaPath;

	@BeforeAll
	public static void init() {
		i18nPath = get("src/main/resources/i18n");
		assertThat(i18nPath).exists();
		refPath = i18nPath.resolve(DEFAULT_LANG_FILE);
		assertThat(refPath).exists();
		guiJavaPath = get("src/main/java");
		assertThat(guiJavaPath).exists();
	}

	@Test
	public void verifyLocales() {
		for (LangLocale lang : NLS.getLangLocales()) {
			Locale locale = lang.get();
			System.out.println("Language: " + locale.getLanguage() + " - " + locale.getDisplayLanguage()
					+ ", country: " + locale.getCountry() + " - " + locale.getDisplayCountry()
					+ ", language tag: " + locale.toLanguageTag());
		}
	}

	@Test
	public void filesExactlyMatch() throws IOException {
		List<String> reference = Files.readAllLines(refPath)
				.stream()
				.map(TestI18n::getPrefix)
				.collect(Collectors.toList());
		try (Stream<Path> list = Files.list(i18nPath)) {
			list.filter(p -> !p.equals(refPath))
					.forEach(path -> compareToReference(path, reference));
		}
	}

	/**
	 * Extract prefix: 'key='
	 */
	private static String getPrefix(String line) {
		if (line.isBlank()) {
			return "";
		}
		int sep = line.indexOf('=');
		if (sep == -1) {
			return line;
		}
		if (line.startsWith("#")) {
			fail(DEFAULT_LANG_FILE + " shouldn't contain commented values: " + line);
		}
		return line.substring(0, sep + 1);
	}

	private void compareToReference(Path path, List<String> reference) {
		try {
			List<String> lines = Files.readAllLines(path);
			for (int i = 0; i < reference.size(); i++) {
				String prefix = reference.get(i);
				if (prefix.isEmpty()) {
					continue;
				}
				if (i >= lines.size()) {
					fail("File '" + path.getFileName() + "' contains unexpected lines at end");
				}
				String line = lines.get(i);
				if (!trimComment(line).startsWith(prefix)) {
					failLine(path, i + 1);
				}
				if (line.startsWith("#")) {
					int sep = line.indexOf('=');
					if (line.substring(sep + 1).isBlank()) {
						fail("File '" + path.getFileName() + "' has empty ref text at line " + (i + 1) + ": " + line);
					}
				}
			}
			if (lines.size() != reference.size()) {
				failLine(path, reference.size());
			}
		} catch (IOException e) {
			fail("Process error ", e);
		}
	}

	private static String trimComment(String string) {
		return string.startsWith("#") ? string.substring(1) : string;
	}

	private void failLine(Path path, int line) {
		fail("I18n file: " + path.getFileName() + " and " + DEFAULT_LANG_FILE + " differ in line " + line);
	}

	@Test
	public void keyIsUsed() throws IOException {
		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(i18nPath.resolve(DEFAULT_LANG_FILE))) {
			properties.load(reader);
		}
		Set<String> keys = new HashSet<>();
		for (Object key : properties.keySet()) {
			keys.add("\"" + key + '"');
		}
		try (Stream<Path> walk = Files.walk(guiJavaPath)) {
			walk.filter(Files::isRegularFile).forEach(p -> {
				try {
					for (String line : Files.readAllLines(p)) {
						keys.removeIf(line::contains);
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}
		assertThat(keys).as("keys not used").isEmpty();
	}
}
