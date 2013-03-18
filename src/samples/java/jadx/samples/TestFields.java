package jadx.samples;

import java.util.Arrays;

public class TestFields extends AbstractTest {

	private final static boolean fbz = false;
	private final static boolean fb = true;
	private final static int fi = 5;
	private final static int fiz = 0;

	private final static String fstr = "final string";

	private final static double fd = 3.14;
	private final static double[] fda = new double[] { 3.14, 2.7 };

	private static int si = 5;

	@Override
	public boolean testRun() throws Exception {
		String str = "" + fbz + fiz + fb + fi + fstr + fd + Arrays.toString(fda) + si;
		return str.equals("false0true5final string3.14[3.14, 2.7]5");
	}

}
