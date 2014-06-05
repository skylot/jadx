package jadx.samples;

public class TestInner extends AbstractTest {

	public static int count = -2;

	public static class MyThread extends Thread {

		public MyThread(String name) {
			super(name);
		}

		@Override
		public void run() {
			count++;
			super.run();
		}
	}

	public static class MyInceptionThread extends MyThread {

		public static class MyThread2 extends Thread {
			@Override
			public void run() {
				count += 2;
			}
		}

		public MyInceptionThread() {
			super("MyInceptionThread");
		}

		@Override
		public void run() {
			MyThread2 thr = new MyThread2();
			thr.start();
			try {
				thr.join();
			} catch (InterruptedException e) {
				assertTrue(false);
			}
		}
	}

	public void func() {
		new Runnable() {
			@Override
			public void run() {
				count += 4;
			}
		}.run();
	}

	public void func2() {
		new Runnable() {
			{
				count += 5;
			}

			@Override
			public void run() {
				count += 6;
			}
		}.run();
	}

	public String func3() {
		return new Object() {
			{
				count += 7;
			}

			@Override
			public String toString() {
				count += 8;
				return Integer.toString(count);
			}
		}.toString();
	}

	@SuppressWarnings("serial")
	public static class MyException extends Exception {
		public MyException(String str, Exception e) {
			super("msg:" + str, e);
		}
	}

	@Override
	public boolean testRun() throws Exception {
		TestInner c = new TestInner();
		TestInner.count = 0;
		c.func();
		c.func2();

		Runnable myRunnable = new Runnable() {
			@Override
			public void run() {
				TestInner.count += 8;
			}
		};
		myRunnable.run();

		MyThread thread = new TestInner.MyThread("my thread");
		thread.start();

		MyInceptionThread thread2 = new TestInner.MyInceptionThread();
		thread2.start();

		thread.join();
		thread2.join();

		assertEquals(func3(), "41");

		return TestInner.count == 41;
	}
}
