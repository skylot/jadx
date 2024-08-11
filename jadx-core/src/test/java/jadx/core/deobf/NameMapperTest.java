package jadx.core.deobf;

import org.junit.jupiter.api.Test;

import static jadx.core.deobf.NameMapper.isValidIdentifier;
import static jadx.core.deobf.NameMapper.removeInvalidChars;
import static jadx.core.deobf.NameMapper.removeInvalidCharsMiddle;
import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class NameMapperTest {

	@Test
	public void validIdentifiers() {
		assertThat(isValidIdentifier("ACls")).isTrue();
	}

	@Test
	public void notValidIdentifiers() {
		assertThat(isValidIdentifier("1cls")).isFalse();
		assertThat(isValidIdentifier("-cls")).isFalse();
		assertThat(isValidIdentifier("A-cls")).isFalse();
	}

	@Test
	public void testRemoveInvalidCharsMiddle() {
		assertThat(removeInvalidCharsMiddle("1cls")).isEqualTo("1cls");
		assertThat(removeInvalidCharsMiddle("-cls")).isEqualTo("cls");
		assertThat(removeInvalidCharsMiddle("A-cls")).isEqualTo("Acls");
	}

	@Test
	public void testRemoveInvalidChars() {
		assertThat(removeInvalidChars("1cls", "C")).isEqualTo("C1cls");
		assertThat(removeInvalidChars("-cls", "C")).isEqualTo("cls");
		assertThat(removeInvalidChars("A-cls", "C")).isEqualTo("Acls");
	}
}
