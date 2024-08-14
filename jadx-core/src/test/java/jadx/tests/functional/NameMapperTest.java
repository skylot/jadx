package jadx.tests.functional;

import org.junit.jupiter.api.Test;

import jadx.core.deobf.NameMapper;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class NameMapperTest {

	@Test
	public void testValidFullIdentifiers() {
		String[] validNames = {
				"C",
				"Cc",
				"b.C",
				"b.Cc",
				"aAa.b.Cc",
				"a.b.Cc",
				"a.b.C_c",
				"a.b.C$c",
				"a.b.C9"
		};
		for (String validName : validNames) {
			assertThat(NameMapper.isValidFullIdentifier(validName)).isTrue();
		}
	}

	@Test
	public void testInvalidFullIdentifiers() {
		String[] invalidNames = {
				"",
				"5",
				"7A",
				".C",
				"b.9C",
				"b..C",
		};
		for (String invalidName : invalidNames) {
			assertThat(NameMapper.isValidFullIdentifier(invalidName)).isFalse();
		}
	}
}
