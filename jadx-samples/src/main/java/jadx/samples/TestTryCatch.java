package jadx.samples;

import java.io.IOException;

public class TestTryCatch extends AbstractTest {

	private static boolean exc(Object obj) throws Exception {
		if (obj == null) {
			throw new Exception("test");
		}
		return (obj instanceof Object);
	}

	private static boolean exc2(Object obj) throws IOException {
		if (obj == null) {
			throw new IOException();
		}
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
			if (obj != null) {
				return true;
			} else {
				return false;
			}
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
			if (!f) {
				res = "f == false";
			}
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
				if (obj == null) {
					obj = new Object();
				}
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
				if (obj == null) {
					res = false;
				}
			}
		}
	}

	private boolean test8(Object obj) {
		this.mDiscovering = false;
		try {
			exc(obj);
		} catch (Exception e) {
			e.toString();
		} finally {
			mDiscovering = true;
		}
		return mDiscovering;
	}

	private boolean test8a(Object obj) {
		this.mDiscovering = false;
		try {
			exc(obj);
		} catch (Exception e) {
			e.toString();
		} finally {
			if (!mDiscovering) {
				mDiscovering = true;
			}
		}
		return mDiscovering;
	}

	private static boolean testSynchronize(Object obj) throws InterruptedException {
		synchronized (obj) {
			if (obj instanceof String) {
				return false;
			}
			obj.wait(5);
		}
		return true;
	}

	// TODO: remove 'synchronized(TestTryCatch.class)' block in decompiled version
	private synchronized static boolean testSynchronize2(Object obj) throws InterruptedException {
		return obj.toString() != null;
	}

	public Object mObject = new Object();
	public boolean mDiscovering = true;

	private boolean testSynchronize3() {
		boolean b = false;
		synchronized (mObject) {
			b = this.mDiscovering;
		}
		return b;
	}

	public int catchInLoop(int i, int j) {
		while (true) {
			try {
				while (i < j) {
					i = j++ / i;
				}
			} catch (RuntimeException e) {
				i = 10;
				continue;
			}
			break;
		}
		return j;
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

		assertTrue(testSynchronize(obj));
		assertFalse(testSynchronize("str"));

		assertTrue(testSynchronize2("str"));
		assertTrue(testSynchronize3());

		assertTrue(test8("a"));
		assertTrue(test8(null));

		assertTrue(test8a("a"));
		assertTrue(test8a(null));

		assertEquals(catchInLoop(1, 0), 0);
		assertEquals(catchInLoop(0, 1), 2);
		assertEquals(catchInLoop(788, 100), 100);
		return true;
	}

	public static void main(String[] args) throws Exception {
		new TestTryCatch().testRun();
	}
}
