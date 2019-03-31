package jadx.tests.integration.variables;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

public class TestVariables7 extends IntegrationTest {

	public static class TestCls {

		public void test() {
			List list;
			synchronized (this) {
				list = new ArrayList();
			}
			for (Object o : list) {
				System.out.println(o);
			}
		}
	}

	@Test
	@NotYetImplemented
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("     list = new ArrayList"));
	}
}
