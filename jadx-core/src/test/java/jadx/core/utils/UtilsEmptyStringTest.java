package jadx.core.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class UtilsEmptyStringTest {

	@Test
	public void cleanObjectNameEmptyString() {
		// cleanObjectName should handle empty string gracefully (return it unchanged),
		// not throw StringIndexOutOfBoundsException on charAt(0)
		assertDoesNotThrow(() -> Utils.cleanObjectName(""));
		assertEquals("", Utils.cleanObjectName(""));
	}

	@Test
	public void cutObjectEmptyString() {
		// cutObject should handle empty string gracefully (return it unchanged),
		// not throw StringIndexOutOfBoundsException on charAt(0)
		assertDoesNotThrow(() -> Utils.cutObject(""));
		assertEquals("", Utils.cutObject(""));
	}

	@Test
	public void cleanObjectNameNormalCase() {
		assertEquals("java.lang.String", Utils.cleanObjectName("Ljava/lang/String;"));
	}

	@Test
	public void cutObjectNormalCase() {
		assertEquals("java/lang/String", Utils.cutObject("Ljava/lang/String;"));
	}
}
