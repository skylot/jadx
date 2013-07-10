package jadx.samples;

import java.util.Arrays;

public class TestFields extends AbstractTest {

	public static class ConstFields {
		public static final boolean BOOL = false;
		public static final int CONST_INT = 56789;
		public static final int ZERO = 0;
		public static final String STR = "string";
		public static final double PI = 3.14;
	}

	private static final boolean fbz = false;
	private static final boolean fb = true;
	private static final int fi = 5;
	private static final int fiz = 0;

	private static final String fstr = "final string";

	private static final double fd = 3.14;
	private static final double[] fda = new double[]{3.14, 2.7};

	private static int si = 5;

	public void testConstsFields() {
		int r = ConstFields.CONST_INT;
		r += ConstFields.BOOL ? 1 : 0;
		r += ConstFields.ZERO * 5;
		r += ConstFields.STR.length() + ConstFields.STR.indexOf('i');
		r += Math.round(ConstFields.PI);
		assertEquals(r, 56801);
	}

	@Override
	public boolean testRun() throws Exception {
		testConstsFields();

		String str = "" + fbz + fiz + fb + fi + fstr + fd + Arrays.toString(fda) + si;
		return str.equals("false0true5final string3.14[3.14, 2.7]5");
	}

	public static void main(String[] args) throws Exception {
		new TestFields().testRun();
	}
}
