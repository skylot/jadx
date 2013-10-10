package jadx.samples;

public class TestTypeResolver extends AbstractTest {

	private final int f1;

	public TestTypeResolver() {
		this.f1 = 2;
	}

	public TestTypeResolver(int b1, int b2) {
		// test 'this' move and constructor invocation on moved register
		this(b1, b2, 0, 0, 0);
	}

	public TestTypeResolver(int a1, int a2, int a3, int a4, int a5) {
		this.f1 = a1;
	}

	public static class TestTernaryInSuper extends TestTypeResolver {

		public TestTernaryInSuper(int c) {
//			super(c > 0 ? c : -c, 1);
		}
	}

	// public static Object testVarsPropagation(int a) {
	// Object b = new Exception();
	// if (a == 5)
	// b = 1;
	// return b;
	// }
	//
	// public Object testMoveThis(int a) {
	// TestTypeResolver t = this;
	// if (a == 0)
	// return t;
	//
	// return t.testMoveThis(--a);
	// }

	@Override
	public boolean testRun() throws Exception {
		// assertTrue((Integer) testVarsPropagation(5) == 1);
		// assertTrue(testVarsPropagation(1).getClass() == Exception.class);
		//
		// assertTrue(testMoveThis(f1) == this);
		return true;
	}

	public static void main(String[] args) throws Exception {
		new TestTypeResolver().testRun();
	}
}
