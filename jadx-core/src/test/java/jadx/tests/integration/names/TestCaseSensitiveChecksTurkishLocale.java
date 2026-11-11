package jadx.tests.integration.names;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestCaseSensitiveChecksTurkishLocale extends SmaliTest {
	/*
	 * com.example.Item and com.example.item - in Turkish locale 'I' is lowercased to dotless 'ı',
	 * so the case-insensitive check must not depend on the default locale.
	 */

	private Locale defaultLocale;

	@BeforeEach
	public void setTurkishLocale() {
		defaultLocale = Locale.getDefault();
		Locale.setDefault(Locale.forLanguageTag("tr-TR"));
	}

	@AfterEach
	public void restoreLocale() {
		Locale.setDefault(defaultLocale);
	}

	@Test
	public void test() {
		args.setFsCaseSensitive(false);

		List<ClassNode> classes = loadFromSmaliFiles();
		assertThat(classes).hasSize(2);

		long distinct = classes.stream()
				.map(cls -> cls.getClassInfo().getAliasFullPath().toLowerCase(Locale.ROOT))
				.distinct()
				.count();
		assertThat(distinct).isEqualTo(2L);
	}
}
