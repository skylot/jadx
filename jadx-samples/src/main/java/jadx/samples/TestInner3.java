package jadx.samples;

public class TestInner3 extends AbstractTest {

	private String i0;

	public class A {
		
		protected String a;
		
		public A() {
			a="";
		}

		public String a() {
			return "";
		}
	}

	public class I0 {
		private String i0;
		private String i1;

		public class I1 {
			private String i0;
			private String i1;
			private String i2;

			public I1() {
				TestInner3.this.i0 = "i0";
				I0.this.i0 = "i1";
				I0.this.i1 = "i2";

				i0 = "i0";
				i1 = "i1";
				i2 = "i2";
			}

			public String i() {

				String result = TestInner3.this.i0 + I0.this.i0 + I0.this.i1 + i0 + i1 + i2;
				
				A a = new A() {

					public String a() {
						TestInner3.this.i0 = "i1";
						I0.this.i0 = "i2";
						I0.this.i1 = "i3";
						I1.this.i0 = "i1";
						I1.this.i1 = "i2";
						I1.this.i2 = "i3";
						a = "a";

						return TestInner3.this.i0 + I0.this.i0 + I0.this.i1 + I1.this.i0 + I1.this.i1 + I1.this.i2 + a;
					}
				};

				return result + a.a();
			}
		}

		public I0() {
			TestInner3.this.i0 = "i-";
			i0 = "i0";
			i1 = "i1";
		}

		public String i() {
			String result = TestInner3.this.i0 + i0 + i1;
			return result + (new I1()).i();
		}
	}

	@Override
	public boolean testRun() throws Exception {
		assertTrue((new I0()).i().equals("i-i0i1i0i1i2i0i1i2i1i2i3i1i2i3a"));
		assertTrue(i0.equals("i1"));

		return true;
	}

	public static void main(String[] args) throws Exception {
		new TestInner2().testRun();
	}

}
