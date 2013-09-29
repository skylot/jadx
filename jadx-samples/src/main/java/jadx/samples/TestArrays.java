package jadx.samples;

public class TestArrays extends AbstractTest {

	public int test1(int i) {
		// fill-array-data
		int[] a = new int[]{1, 2, 3, 5};
		return a[i];
	}

	public int test2(int i) {
		// filled-new-array
		int[][] a = new int[i][i + 1];
		return a.length;
	}

	public int test3(int i) {
		// filled-new-array/range
		boolean[][][][][][][][] a = new boolean[i][i][i][i][i][i][i][i];
		return a.length;
	}

	private static Object test4(int type) {
		if (type == 1) {
			return new int[]{1, 2};
		} else if (type == 2) {
			return new float[]{1, 2};
		} else if (type == 3) {
			return new short[]{1, 2};
		} else if (type == 4) {
			return new byte[]{1, 2};
		} else {
			return null;
		}
	}

	@Override
	public boolean testRun() throws Exception {
		assertEquals(test1(2), 3);
		assertEquals(test2(2), 2);
		assertEquals(test3(2), 2);

		assertTrue(test4(4) instanceof byte[]);
		return true;
	}

	public static void main(String[] args) throws Exception {
		new TestArrays().testRun();
	}
}
