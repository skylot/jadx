package jadx.samples;

public class TestStringProcessing extends AbstractTest {

	public void testStringEscape() {
		String str = "test\tstr\n";
		assertTrue(str.length() == 9);

		str = "test\bunicode\u1234";
		assertTrue(str.charAt(4) == '\b');
	}

	public void testStringConcat() {
		String s = "1";
		assertEquals("a" + s, "a1");
	}

	@Override
	public boolean testRun() throws Exception {
		testStringEscape();
		testStringConcat();
		return true;
	}
}
