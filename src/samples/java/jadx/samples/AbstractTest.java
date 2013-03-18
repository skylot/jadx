package jadx.samples;

public abstract class AbstractTest {

	public abstract boolean testRun() throws Exception;

	public static void assertTrue(boolean condition) {
		if (!condition) {
			throw new AssertionError();
		}
	}

	public static void assertEquals(int a1, int a2) {
		if (a1 != a2) {
			throw new AssertionError(a1 + " != " + a2);
		}
	}
}
