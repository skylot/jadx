package jadx.samples;

public class TestCF2 extends AbstractTest {
	private final Object readyMutex = new Object();
	private boolean ready = false;

	public int simpleLoops() throws InterruptedException {
		int[] a = new int[]{1, 2, 4, 6, 8};
		int b = 0;
		for (int i = 0; i < a.length; i++) {
			b += a[i];
		}
		for (long i = b; i > 0; i--) {
			b += i;
		}
		return b;
	}

	/**
	 * Test infinite loop
	 */
	public void run() throws InterruptedException {
		while (true) {
			if (!ready) {
				readyMutex.wait();
			}
			ready = false;
			func();
		}
	}

	private void func() {
		ready = true;
	}

	public void doWhile() throws InterruptedException {
		int i = 3;
		do {
			func();
			i++;
		} while (i < 5);
	}

	public void doWhile2(long k) throws InterruptedException {
		if (k > 5) {
			long i = 3;
			do {
				func();
				i++;
			} while (i < 5);
		}
	}

	public void doWhile3(int k) throws InterruptedException {
		int i = 3;
		do {
			if (k > 9) {
				func();
			}
			i++;
		} while (i < 5);
	}

	public int doWhileBreak(int k) throws InterruptedException {
		int i = 3;
		do {
			if (k > 9) {
				i = 0;
				break;
			}
			i++;
		} while (i < 5);

		return i;
	}

	public int doWhileContinue(int k) throws InterruptedException {
		int i = 0;
		do {
			if (k > 9) {
				i = k + 1;
				continue;
			}
			i++;
		} while (i < k);
		return i;
	}

	public void doWhileReturn2(boolean k) throws InterruptedException {
		int i = 3;
		do {
			if (k) {
				return;
			}
			i++;
		} while (i < 5);
	}

	public void whileIterator(String[] args, int k) throws InterruptedException {
		for (String arg : args) {
			if (arg.length() > 9) {
				func();
			}
		}
	}

	@Override
	public boolean testRun() throws Exception {
		assertEquals(simpleLoops(), 252);
		// TODO add checks
		return true;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("TestCF2: " + new TestCF2().testRun());
	}
}
