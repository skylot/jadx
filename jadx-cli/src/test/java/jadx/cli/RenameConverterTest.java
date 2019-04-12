package jadx.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jadx.api.JadxArgs.RENAME;
import jadx.cli.JadxCLIArgs.RenameConverter;

public class RenameConverterTest {

	private RenameConverter converter;

	@BeforeEach
	public void init() {
		converter = new RenameConverter("someParam");
	}

	@Test
	public void all() {
		Set<RENAME> set = converter.convert("all");
		assertEquals(3, set.size());
		assertTrue(set.contains(RENAME.CASE));
		assertTrue(set.contains(RENAME.VALID));
		assertTrue(set.contains(RENAME.PRINTABLE));
	}

	@Test
	public void none() {
		Set<RENAME> set = converter.convert("none");
		assertTrue(set.isEmpty());
	}

	@Test
	public void wrong() {
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
				() -> converter.convert("wrong"),
				"Expected convert() to throw, but it didn't");

		assertEquals("wrong is unknown for parameter someParam, "
				+ "possible values are 'case', 'valid' and 'printable'",
				thrown.getMessage());
	}
}
