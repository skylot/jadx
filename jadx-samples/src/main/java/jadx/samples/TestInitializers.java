package jadx.samples;

public class TestInitializers extends AbstractTest {

	private static String a;
	private static int counter;
	private A c_a;
	
	public static class A {
		public static String a;
		
		static {
			a = "a1";
		}
		
		public boolean z() {
			return true;
		}
	}

	public class B {
		private int b;
		private int bbb;

		public B() {
			if (c_a.z()) {
				b = -1;
			} else {
				b = 1;
			}
		}
		
		public B(int _b) {
			b = _b;
		}
		
		public void setB(int _b) {
			b = _b;
		}

		public int getB() {
			return b;
		}
		
		public int getBBB() {
			return bbb;
		}
		
		{
			bbb = 123;
		}
	}

	static {
		a = "a0";
		counter = 0;
	}
	
	{
		c_a = new A();
	}

	@Override
	public boolean testRun() throws Exception {
		assertTrue(counter == 0);
		assertTrue(a.equals("a0"));
		assertTrue(A.a.equals("a1"));

		B b1 = new B() {
			{
				TestInitializers.counter++;
				setB(TestInitializers.counter);
			}
		};
		assertTrue(b1.getB() == 1);

		B b2 = new B() {
			@SuppressWarnings("unused")
			private int bb;

			public int getB() {
				return super.getB();
			}

			{
				bb = 100;
			}
		};
		assertTrue(b2.getB() == -1);

		assertTrue((new B()).getB() == -1);
		assertTrue(counter == 1);
		
		B b3 = new B(3);
		assertTrue((b3.getB() == 3) && (b3.getBBB() == 123));
		
		return true;
	}

	public static void main(String[] args) throws Exception {
		new TestInitializers().testRun();
	}

}
