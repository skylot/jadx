package jadx.tests.functional;

import org.junit.jupiter.api.Test;

import jadx.core.deobf.NameMapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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
			assertThat(NameMapper.isValidFullIdentifier(validName), is(true));
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
			assertThat(NameMapper.isValidFullIdentifier(invalidName), is(false));
		}
	}
}
