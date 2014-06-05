package jadx.samples;

import java.util.Arrays;

public class TestInvoke extends AbstractTest {
	private int f;

	public TestInvoke() {
		this(-1);
	}

	public TestInvoke(int f) {
		this.f = f;
	}

	private void parse(String[] args) {
		if (args.length > 0) {
			f = Integer.parseInt(args[0]);
		} else {
			f = 20;
		}
	}

	public int getF() {
		return f;
	}

	private boolean testVarArgs(String s1, String... args) {
		String str = Arrays.toString(args);
		return s1.length() + str.length() > 0;
	}

	private String testVarArgs2(char[]... args) {
		String s = "";
		for (char[] ca : args) {
			s += new String(ca);
		}
		return s;
	}

	private String testSameArgTypes(String s1, String s2) {
		if (s1.equals(s2)) {
			return null;
		}
		return s1;
	}

	@Override
	public boolean testRun() throws Exception {
		TestInvoke inv = new TestInvoke();

		inv.parse(new String[]{"12", "35"});
		assertTrue(inv.getF() == 12);
		inv.parse(new String[0]);
		assertTrue(inv.getF() == 20);

		assertTrue(inv.testVarArgs("a", "2", "III"));
		assertTrue(inv.testVarArgs2("a".toCharArray(), new char[]{'1', '2'}).equals("a12"));

		assertEquals(testSameArgTypes("a", "b"), "a");
		return true;
	}

	public static void main(String[] args) throws Exception {
		new TestInvoke().testRun();
	}
}
