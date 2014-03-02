package jadx.samples;

import jadx.samples.otherpkg.C.E;
import jadx.samples.otherpkg.D;

public class TestImports2 extends AbstractTest {

	public Object f1() {
		return new E() {
			@Override
			public String toString() {
				return "C.E";
			}
		};
	}

	public Object f2() {
		return new D.E() {
			@Override
			public String toString() {
				return "D.E";
			}
		};
	}

	public static class X1 extends E {
	}

	public static class X2 extends D.E {
	}

	@Override
	public boolean testRun() {
		return true;
	}

	public static void main(String[] args) {
		new TestImports2().testRun();
	}
}
