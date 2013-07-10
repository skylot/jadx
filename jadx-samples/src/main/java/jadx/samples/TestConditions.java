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
		if(num == 4 || num == 6){
			return String.valueOf(num).equals("4");
		}
		if(num == 5) {
			return true;
		}
		return this.toString().equals("a");
	}

	@Override
	public boolean testRun() throws Exception {
		TestConditions c = new TestConditions();

		assertEquals(c.test1(50), 50);
		assertEquals(c.test1(60), 61);

		assertEquals(c.test1a(50), 49);
		assertEquals(c.test1a(60), 60);

		assertEquals(c.test1b(60), 61);
		assertEquals(c.test1b(62), 62);

		assertTrue(c.test1c(4));
		assertFalse(c.test1c(5));
		return true;
	}

	public static void main(String[] args) throws Exception {
		new TestConditions().testRun();
	}
}
