package jadx.tests.integration.inner;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestInnerClass5 extends IntegrationTest {

	public static class TestCls {

		private String i0;

		public class A {

			protected String a;

			public A() {
				a = "";
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
					TestCls.this.i0 = "i0";
					I0.this.i0 = "i1";
					I0.this.i1 = "i2";

					i0 = "i0";
					i1 = "i1";
					i2 = "i2";
				}

				public String i() {

					String result = TestCls.this.i0 + I0.this.i0 + I0.this.i1 + i0 + i1 + i2;

					A a = new A() {

						public String a() {
							TestCls.this.i0 = "i1";
							I0.this.i0 = "i2";
							I0.this.i1 = "i3";
							I1.this.i0 = "i1";
							I1.this.i1 = "i2";
							I1.this.i2 = "i3";
							a = "a";

							return TestCls.this.i0 + I0.this.i0 + I0.this.i1 + I1.this.i0 + I1.this.i1 + I1.this.i2 + a;
						}
					};

					return result + a.a();
				}
			}

			public I0() {
				TestCls.this.i0 = "i-";
				i0 = "i0";
				i1 = "i1";
			}

			public String i() {
				String result = TestCls.this.i0 + i0 + i1;
				return result + new I1().i();
			}
		}

		public void check() throws Exception {
			assertTrue(new I0().i().equals("i-i0i1i0i1i2i0i1i2i1i2i3i1i2i3a"));
			assertTrue(i0.equals("i1"));
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("public class I0 {"));
		assertThat(code, containsOne("public class I1 {"));
	}
}
