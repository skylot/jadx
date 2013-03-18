package jadx.samples;

public class TestStringProcessing extends AbstractTest {

	@Override
	public boolean testRun() {
		String str = "test\tstr\n";
		assertTrue(str.length() == 9);

		str = "test\bunicode\u1234";
		assertTrue(str.charAt(4) == '\b');
		return true;
	}

}
