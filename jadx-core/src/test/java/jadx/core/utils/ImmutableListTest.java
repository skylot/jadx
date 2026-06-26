package jadx.core.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImmutableListTest {

	@Test
	public void lastIndexOfElementOnlyAtIndex0() {
		String[] arr = new String[] { "a", "b", "c" };
		ImmutableList<String> list = new ImmutableList<>(arr);
		// "a" is at index 0 only; lastIndexOf should return 0
		assertEquals(0, list.lastIndexOf("a"));
	}

	@Test
	public void lastIndexOfElementAtIndex0AndLater() {
		String[] arr = new String[] { "a", "b", "a" };
		ImmutableList<String> list = new ImmutableList<>(arr);
		// "a" is at index 0 and 2; lastIndexOf should return 2
		assertEquals(2, list.lastIndexOf("a"));
	}

	@Test
	public void lastIndexOfElementNotPresent() {
		String[] arr = new String[] { "a", "b", "c" };
		ImmutableList<String> list = new ImmutableList<>(arr);
		assertEquals(-1, list.lastIndexOf("z"));
	}

	@Test
	public void lastIndexOfSingleElementList() {
		String[] arr = new String[] { "only" };
		ImmutableList<String> list = new ImmutableList<>(arr);
		assertEquals(0, list.lastIndexOf("only"));
	}
}
