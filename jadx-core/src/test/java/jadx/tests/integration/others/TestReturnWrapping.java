package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestReturnWrapping extends IntegrationTest {
	public static class TestCls {

		public static int f1(int arg0) {
			switch (arg0) {
				case 1:
					return 255;
			}
			return arg0 + 1;
		}

		public static Object f2(Object arg0, int arg1) {
			Object ret = null;
			int i = arg1;
			if (arg0 == null) {
				return ret + Integer.toHexString(i);
			}
			i++;
			try {
				ret = new Object().getClass();
			} catch (Exception e) {
				ret = "Qwerty";
			}
			return i > 128 ? arg0.toString() + ret.toString() : i;
		}

		public static int f3(int arg0) {
			while (arg0 > 10) {
				int abc = 951;
				if (arg0 == 255) {
					return arg0 + 2;
				}
				arg0 -= abc;
			}
			return arg0;
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("return 255;")
				.contains("return arg0 + 1;").contains("return i > 128 ? arg0.toString() + ret.toString() : Integer.valueOf(i);")
				.contains("return arg0 + 2;")
				.contains("arg0 -= 951;");
	}
}
