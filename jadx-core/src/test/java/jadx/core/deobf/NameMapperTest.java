package jadx.core.deobf;

import org.junit.jupiter.api.Test;

import static jadx.core.deobf.NameMapper.isValidIdentifier;
import static jadx.core.deobf.NameMapper.removeInvalidChars;
import static jadx.core.deobf.NameMapper.removeInvalidCharsMiddle;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class NameMapperTest {

	@Test
	public void validIdentifiers() {
		assertThat(isValidIdentifier("ACls"), is(true));
	}

	@Test
	public void notValidIdentifiers() {
		assertThat(isValidIdentifier("1cls"), is(false));
		assertThat(isValidIdentifier("-cls"), is(false));
		assertThat(isValidIdentifier("A-cls"), is(false));
	}

	@Test
	public void testRemoveInvalidCharsMiddle() {
		assertThat(removeInvalidCharsMiddle("1cls"), is("1cls"));
		assertThat(removeInvalidCharsMiddle("-cls"), is("cls"));
		assertThat(removeInvalidCharsMiddle("A-cls"), is("Acls"));
	}

	@Test
	public void testRemoveInvalidChars() {
		assertThat(removeInvalidChars("1cls", "C"), is("C1cls"));
		assertThat(removeInvalidChars("-cls", "C"), is("cls"));
		assertThat(removeInvalidChars("A-cls", "C"), is("Acls"));
	}
}
