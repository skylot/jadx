package jadx.samples;

public class TestCF extends AbstractTest {

	public int test1(int a) {
		if (a > 0) {
			return 1;
		} else {
			a += 2;
			return a * 3;
		}
	}

	public int test1a(int a) {
		if (a > 0) {
			a++;
		}
		a *= 2;
		return a + 3;

	}

	public int test1b(int a) {
		if (a > 0) {
			if (a < 5) {
				a++;
			} else {
				a -= 2;
			}
		}
		a *= 2;
		return a + 3;

	}

	public int test1c(int a, int b) {
		if (a > 0) {
			long c = 5;
			a = (int) (a + c);
		} else {
			double f = 7.7;
			a *= f;
		}
		return a + b;

	}

	public int test2(int a, int b) {
		int c = a + b;
		for (int i = a; i < b; i++) {
			c *= 2;
		}
		c--;
		return c;
	}

	public int test2a(int a, int b) {
		int c = a + b;
		for (int i = a; i < b; i++) {
			if (i == 7) {
				c += 2;
			} else {
				c *= 2;
			}
		}
		c--;
		return c;
	}

	public int test3(int a, int b) {
		int c = 0;
		for (int i = a; i < b; i++) {
			int z = a * i + 5;
			if (i == 7) {
				c += z + a;
			} else {
				c *= z + b;
			}
		}
		return c;
	}

	public int test4(int a, int b) {
		int c = 0;
		for (int i = a; i < b; i++) {
			int z = (i == 7 ? a : b);
			c *= z + b;
			if (i == 7) {
				c += z + a;
			} else {
				c *= z + b;
			}
		}
		return c;
	}

	public int test5(int a, int b) {
		int c = b;
		do {
			int z = c + a;
			if (z >= 7) {
				break;
			}
			c = z;
		} while (true);
		return c;
	}

	public int test6(int a, int b) {
		int c = b;
		int z;
		while ((z = c + a) >= 7) {
			c = z;
		}
		return c;
	}

	public int test7(int a, int b) {
		int c = b;
		int z;

		do {
			z = c + a;
			if (z >= 7) {
				break;
			}
			c = z;
		} while (true);

		while ((z = c + a) >= 7) {
			c = z;
		}
		return c;
	}

	public int testIfElse(String str) {
		int r;
		if (str.equals("a")) {
			r = 1;
		} else if (str.equals("b")) {
			r = 2;
		} else if (str.equals("3")) {
			r = 3;
		} else if (str.equals("$")) {
			r = 4;
		} else {
			r = -1;
		}

		r = r * 10;
		return Math.abs(r);
	}

	public int testIfElse2(String str) {
		String a;
		if (str.length() == 5) {
			a = new String("1");
			a.trim();
			a.length();
		}
		a = new String("22");
		a.toLowerCase();
		return a.length();
	}

	public void testInfiniteLoop() {
		while (true) {
			System.out.println("test");
		}
	}

	public static void test_hello(String[] args) {
		System.out.println("Hello world!");
	}

	public static void test_print(String[] args) {
		for (String arg : args) {
			System.out.println(arg);
		}
	}

	@Override
	public boolean testRun() throws Exception {
		TestCF c = new TestCF();
		assertEquals(c.test1(1), 1);
		assertEquals(c.test1(-1), 3);

		assertEquals(c.test1a(12), 29);

		assertEquals(c.test1b(-1), 1);
		assertEquals(c.test1b(3), 11);
		assertEquals(c.test1b(12), 23);

		assertEquals(c.test1c(-1, 1), -6);
		assertEquals(c.test1c(3, 2), 10);

		assertEquals(c.test2(2, 4), 23);
		assertEquals(c.test2(6, 4), 9);

		assertEquals(c.test2a(5, 9), 115);
		assertEquals(c.test2a(8, 23), 1015807);

		assertEquals(c.test3(5, 9), 2430);
		assertEquals(c.test3(8, 23), 0);

		assertEquals(c.test4(5, 9), 3240);
		assertEquals(c.test4(8, 15), 0);

		assertEquals(c.testIfElse("b"), 20);
		assertEquals(c.testIfElse("c"), 10);

		assertEquals(c.testIfElse2("12345"), 2);
		return true;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("TestCF: " + new TestCF().testRun());
	}
}
