package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.IntegrationTest;

public class TestCast extends IntegrationTest {

	public static class TestCls {

		public void test(boolean a) {
			write(a ? (byte) 1 : (byte) 0);
		}

		public void write(byte b) {
		}
	}

	@Test
	@NotYetImplemented
	public void test() {
		getClassNode(TestCls.class);
	}
}
