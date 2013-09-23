package jadx.samples;

/**
 * Failed tests for current jadx version
 */
public class TestConditions extends AbstractTest {

	public int test1(int num) {
		boolean inRange = (num >= 59 && num <= 66);
		if (inRange) {
			num++;
		}
		return num;
	}

	public int test1a(int num) {
		boolean notInRange = (num < 59 || num > 66);
		if (notInRange) {
			num--;
		}
		return num;
	}

	public int test1b(int num) {
		boolean inc = (num >= 59 && num <= 66 && num != 62);
		if (inc) {
			num++;
		}
		return num;
	}

	public boolean test1c(int num) {
		return num == 4 || num == 6;
	}

	public boolean test2(int num) {
		if (num == 4 || num == 6) {
			return String.valueOf(num).equals("4");
		}
		if (num == 5) {
			return true;
		}
		return this.toString().equals("a");
	}

	public void test3(boolean a, boolean b) {
		if (a || b) {
			throw new RuntimeException();
		}
		test1(0);
	}

	public boolean accept(String name) {
		return name.startsWith("Test") && name.endsWith(".class") && !name.contains("$");
	}

	@Override
	public boolean testRun() throws Exception {
		assertEquals(test1(50), 50);
		assertEquals(test1(60), 61);

		assertEquals(test1a(50), 49);
		assertEquals(test1a(60), 60);

		assertEquals(test1b(60), 61);
		assertEquals(test1b(62), 62);

		assertTrue(test1c(4));
		assertFalse(test1c(5));

		assertTrue(accept("Test.class"));

		test3(false, false);
		return true;
	}

	public static void main(String[] args) throws Exception {
		new TestConditions().testRun();
	}
}
