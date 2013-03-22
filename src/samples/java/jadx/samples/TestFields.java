package jadx.samples;

import java.util.Arrays;

public class TestFields extends AbstractTest {

	private static final boolean fbz = false;
	private static final boolean fb = true;
	private static final int fi = 5;
	private static final int fiz = 0;

	private static final String fstr = "final string";

	private static final double fd = 3.14;
	private static final double[] fda = new double[] { 3.14, 2.7 };

	private static int si = 5;

	@Override
	public boolean testRun() throws Exception {
		String str = "" + fbz + fiz + fb + fi + fstr + fd + Arrays.toString(fda) + si;
		return str.equals("false0true5final string3.14[3.14, 2.7]5");
	}

}
