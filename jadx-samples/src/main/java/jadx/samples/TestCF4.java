package jadx.samples;

public class TestCF4 extends AbstractTest {

	int c;
	String d;
	String f;

	public void testComplexIf(String a, int b) {
		if (d == null || (c == 0 && b != -1 && d.length() == 0)) {
			c = a.codePointAt(c);
		} else {
			if (a.length() != 2) {
				c = f.compareTo(a);
			}
		}
	}

	public void checkComplexIf() {
		d = null;
		f = null;
		c = 2;
		testComplexIf("abcdef", 0);
		assertEquals(c, (int) 'c');

		d = "";
		f = null;
		c = 0;
		testComplexIf("abcdef", 0);
		assertEquals(c, (int) 'a');

		d = "";
		f = "1";
		c = 777;
		testComplexIf("ab", -1);
		assertEquals(c, 777);
	}

	@Override
	public boolean testRun() throws Exception {
		checkComplexIf();
		return true;
	}

	public static void main(String[] args) throws Exception {
		new TestCF4().testRun();
	}
}
