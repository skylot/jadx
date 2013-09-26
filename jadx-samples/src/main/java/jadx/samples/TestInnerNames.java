package jadx.samples;

public class TestInnerNames extends AbstractTest {

	public int D;

	public class A extends TestInner.MyThread {
		public A(String name) {
			super(name);
		}
	}

	public class B extends A {
		public B(String name) {
			super(name);
		}

		public class C extends TestInner2.B {
		}
	}

	public class C extends TestInner2.B {
	}

	public class D extends TestInner2.D {
	}

	@Override
	public boolean testRun() throws Exception {
		return true;
	}

	public static void main(String[] args) throws Exception {
		new TestInnerNames().testRun();
	}
}
