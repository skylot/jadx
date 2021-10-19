package jadx.cli;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jadx.api.JadxArgs.RenameEnum;
import jadx.cli.JadxCLIArgs.RenameConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RenameConverterTest {

	private RenameConverter converter;

	@BeforeEach
	public void init() {
		converter = new RenameConverter("someParam");
	}

	@Test
	public void all() {
		Set<RenameEnum> set = converter.convert("all");
		assertEquals(3, set.size());
		assertTrue(set.contains(RenameEnum.CASE));
		assertTrue(set.contains(RenameEnum.VALID));
		assertTrue(set.contains(RenameEnum.PRINTABLE));
	}

	@Test
	public void none() {
		Set<RenameEnum> set = converter.convert("none");
		assertTrue(set.isEmpty());
	}

	@Test
	public void wrong() {
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
				() -> converter.convert("wrong"),
				"Expected convert() to throw, but it didn't");

		assertEquals("'wrong' is unknown for parameter someParam, possible values are case, valid, printable",
				thrown.getMessage());
	}
}
