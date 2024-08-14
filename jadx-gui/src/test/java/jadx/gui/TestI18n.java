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
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jadx.gui.utils.LangLocale;
import jadx.gui.utils.NLS;

import static java.nio.file.Files.exists;
import static java.nio.file.Paths.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class TestI18n {

	private static Path guiJavaPath;
	private static Path i18nPath;

	private List<String> reference;
	private String referenceName;

	@BeforeAll
	public static void init() {
		i18nPath = get("src/main/resources/i18n");
		assertThat(exists(i18nPath)).isTrue();
		guiJavaPath = get("src/main/java");
		assertThat(exists(guiJavaPath)).isTrue();
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
		try (Stream<Path> list = Files.list(i18nPath)) {
			list.forEach(p -> {
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
