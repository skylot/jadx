package jadx.samples;

public class TestSwitch extends AbstractTest {

	public static int test1(int i) {
		int k = i * 4;

		switch (k) {
			case 1:
				return 0;
			case 10:
				return 1;
			case 100:
				return 2;
			case 1000:
				return 3;
		}
		i -= 77;
		return i;
	}

	public static int test2(int i) {
		int k = i;
		switch (k) {
			case 1:
				return 0;
			case 2:
				return 1;
			case 3:
				return 2;
			case 5:
				return 3;
			case 7:
				return 4;
			case 9:
				return 5;
		}
		i /= 2;
		return -i;
	}

	public static int test3(int i, int j) {
		int k = i;
		switch (k) {
			case 1:
				if (j == 0) {
					return 0;
				} else {
					return -1;
				}
			case 2:
				return 1;
		}
		return -1;
	}

	public static int test4(int i) {
		int k = i;
		switch (k) {
			case 1:
				throw new RuntimeException("test4");
			case 2:
				return 1;
		}
		return -1;
	}

	@SuppressWarnings("fallthrough")
	public static int test5(int i, int b) {
		int k = i;
		switch (k) {
			case 1:
				if (b == 0) {
					return 3;
				}

			case 2:
				b++;
				return b;
		}
		return -1;
	}

	public String escape(String str) {
		int len = str.length();
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			char c = str.charAt(i);
			switch (c) {
				case '.':
				case '/':
					sb.append('_');
					break;

				case ']':
					sb.append('A');
					break;

				case '?':
					break;

				default:
					sb.append(c);
					break;
			}
		}
		return sb.toString();
	}

	@Override
	public boolean testRun() {
		assertTrue(test1(25) == 2);
		assertTrue(test2(5) == 3);
		assertTrue(test3(1, 0) == 0);
		assertTrue(test4(2) == 1);
		assertEquals(escape("a.b/c]d?e"), "a_b_cAde");
		return true;
	}

	public static void main(String[] args) {
		new TestSwitch().testRun();
	}
}
