package jadx.samples;

import java.io.IOException;

public class TestTryCatch extends AbstractTest {

	private static boolean exc(Object obj) throws Exception {
		if (obj == null)
			throw new Exception("test");
		return (obj instanceof Object);
	}

	private static boolean exc2(Object obj) throws IOException {
		if (obj == null)
			throw new IOException();
		return true;
	}

	private static boolean test0(Object obj) {
		try {
			synchronized (obj) {
				obj.wait(5);
			}
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}

	private static boolean test1(Object obj) {
		boolean res = false;
		try {
			res = exc(obj);
		} catch (Exception e) {
			return false;
		}
		return res;
	}

	private static boolean test2(Object obj) {
		try {
			return exc(obj);
		} catch (Exception e) {
			if (obj != null)
				return true;
			else
				return false;
		}
	}

	private static boolean test3(Object obj) {
		boolean res = false;
		try {
			res = exc(obj);
		} catch (Exception e) {
			res = false;
		} finally {
			test0(obj);
		}
		return res;
	}

	private static String test4(Object obj) {
		String res = "good";
		try {
			res += exc(obj);
			exc2("a");
		} catch (IOException e) {
			res = "io exc";
		} catch (Exception e) {
			res = "exc";
		}
		return res;
	}

	private static String test5(Object obj) {
		String res = "good";
		try {
			res = "" + exc(obj);
			boolean f = exc2("a");
			if (!f)
				res = "f == false";
		} catch (Exception e) {
			res = "exc";
		}
		return res;
	}

	private static boolean test6(Object obj) {
		boolean res = false;
		while (true) {
			try {
				res = exc2(obj);
				return res;
			} catch (IOException e) {
				res = true;
			} catch (Throwable e) {
				if (obj == null)
					obj = new Object();
			}
		}
	}

	private static boolean test7() {
		boolean res = false;
		Object obj = null;
		while (true) {
			try {
				res = exc2(obj);
				return res;
			} catch (IOException e) {
				res = true;
				obj = new Object();
			} catch (Throwable e) {
				if (obj == null)
					res = false;
			}
		}
	}

	private static boolean testSynchronize(Object obj) throws InterruptedException {
		synchronized (obj) {
			if (obj instanceof String)
				return false;
			obj.wait(5);
		}
		return true;
	}

	private synchronized static boolean testSynchronize2(Object obj) throws InterruptedException {
		return obj.toString() != null;
	}

	@Override
	public boolean testRun() throws Exception {
		Object obj = new Object();
		assertTrue(test0(obj));
		assertTrue(test1(obj));
		assertTrue(test2(obj));
		assertTrue(test3(obj));
		assertTrue(test4(obj) != null);
		assertTrue(test5(null) != null);
		assertTrue(test6(obj));
		assertTrue(test7());

		assertTrue(testSynchronize(obj) == true);
		assertTrue(testSynchronize("str") == false);

		assertTrue(testSynchronize2("str"));
		return true;
	}

	public static void main(String[] args) throws Exception {
		new TestTryCatch().testRun();
	}
}
