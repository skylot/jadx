package jadx.samples;

public class TestInner2 extends AbstractTest {

	private String a;

	public class A {
		public A() {
			a = "a";
		}

		public String a() {
			return a;
		}
	}

	private static String b;

	public static class B {
		public B() {
			b = "b";
		}

		public String b() {
			return b;
		}
	}

	@Override
	public boolean testRun() throws Exception {
		assertTrue((new A()).a().equals("a"));
		assertTrue(a.equals("a"));
		assertTrue((new B()).b().equals("b"));
		assertTrue(b.equals("b"));
		return true;
	}

	public static void main(String[] args) throws Exception {
		new TestInner2().testRun();
	}
}
