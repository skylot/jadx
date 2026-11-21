package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTypeResolver27 extends IntegrationTest {

	public static class TestCls {
		public static A test(String[] args) {
			A a = new A();
			int i = 0;
			while (i < args.length) {
				String arg = args[i++];
				switch (arg) {
					case "a0":
						a = a.set(args[i++]);
						if (a != null) {
							a.mark(0);
						}
						break;

					case "a1":
						a = a.set(args[i++]);
						if (a != null) {
							a.mark(1);
						}
						break;

					case "a2":
						a = a.set(args[i++]);
						if (a != null) {
							a.mark(2);
						}
						break;

					case "a3":
						a = a.set(args[i++]);
						if (a != null) {
							a.mark(3);
						}
						break;

					case "a4":
						a = a.set(args[i++]);
						if (a != null) {
							a.mark(4);
						}
						break;

					case "a5":
						a = a.set(args[i++]);
						if (a != null) {
							a.mark(5);
						}
						break;

					case "a6":
						a = a.set(args[i++]);
						if (a != null) {
							a.mark(6);
						}
						break;

					case "a7":
						a = a.set(args[i++]);
						if (a != null) {
							a.mark(7);
						}
						break;

					case "a8":
						a = a.set(args[i++]);
						if (a != null) {
							a.mark(8);
						}
						break;

					case "a9":
						a = a.set(args[i++]);
						if (a != null) {
							a.mark(9);
						}
						break;

					case "a10":
						a = a.set(args[i++]);
						if (a != null) {
							a.mark(10);
						}
						break;

					case "a11":
						a = a.set(args[i++]);
						if (a != null) {
							a.mark(11);
						}
						break;

					default:
						a.mark(-1);
						break;
				}
			}
			return a;
		}

		public static class A {
			private int value;

			public A set(String s) {
				value += s.length();
				return this;
			}

			public void mark(int value) {
				this.value += value;
			}
		}
	}

	@Test
	public void test() {
		args.setTypeUpdatesLimitCount(1);
		noDebugInfo();
		disableCompilation();
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("type inference failed")
				.doesNotContain("Type inference failed")
				.doesNotContain("Types fix failed")
				.containsOne("return a;");
	}
}
