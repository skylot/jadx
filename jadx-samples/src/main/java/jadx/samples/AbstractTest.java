package jadx.samples;

public abstract class AbstractTest {

	public abstract boolean testRun() throws Exception;

	public static void assertTrue(boolean condition) {
		if (!condition) {
			throw new AssertionError();
		}
	}

	public static void assertFalse(boolean condition) {
		if (condition) {
			throw new AssertionError();
		}
	}

	public static void assertTrue(boolean condition, String msg) {
		if (!condition) {
			throw new AssertionError(msg);
		}
	}

	public static void assertEquals(int a1, int a2) {
		if (a1 != a2) {
			throw new AssertionError(a1 + " != " + a2);
		}
	}

	public static void assertEquals(float a1, float a2) {
		if (Float.compare(a1, a2) != 0) {
			throw new AssertionError(a1 + " != " + a2);
		}
	}

	public static void assertEquals(Object a1, Object a2) {
		if (a1 == null) {
			if (a2 != null) {
				throw new AssertionError(a1 + " != " + a2);
			}
		} else if (!a1.equals(a2)) {
			throw new AssertionError(a1 + " != " + a2);
		}
	}

	public static void fail() {
		throw new AssertionError();
	}
}
