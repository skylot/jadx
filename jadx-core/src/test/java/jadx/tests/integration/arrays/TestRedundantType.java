package jadx.tests.integration.arrays;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

public class TestRedundantType extends IntegrationTest {

	public static class TestCls {

		public byte[] method() {
	        return new byte[]{10, 11, 12};
		}
	}

	@Test
	@NotYetImplemented
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("return new byte[]{10, 11, 12};"));
	}

	public static class TestCls2 {

		public byte[] method() {
			byte[] arr = new byte[3];
			arr[2] = 10;
	        return arr;
		}
	}

	@Test
	@NotYetImplemented
	public void test2() {
		ClassNode cls = getClassNode(TestCls2.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("(byte) 10")));
	}
}
