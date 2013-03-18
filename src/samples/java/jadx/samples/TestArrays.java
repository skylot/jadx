package jadx.samples;

public class TestArrays extends AbstractTest {

	public int test1(int i) {
		// fill-array-data
		int[] a = new int[] { 1, 2, 3, 5 };
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

	@Override
	public boolean testRun() throws Exception {
		assertEquals(test1(2), 3);
		assertEquals(test2(2), 2);
		assertEquals(test3(2), 2);
		return true;
	}

	public static void main(String[] args) throws Exception {
		new TestArrays().testRun();
	}
}
