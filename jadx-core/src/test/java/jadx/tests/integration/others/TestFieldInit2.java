package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsLines;
import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestFieldInit2 extends IntegrationTest {

	public static class TestCls {

		public interface BasicAbstract {
			void doSomething();
		}

		public BasicAbstract x = new BasicAbstract() {
			@Override
			public void doSomething() {
				y = 1;
			}
		};
		public int y = 0;

		public TestCls() {
		}

		public TestCls(int z) {
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("x = new BasicAbstract() {"));
		assertThat(code, containsOne("y = 0;"));
		assertThat(code, containsLines(1, "public TestFieldInit2$TestCls(int z) {", "}"));
	}
}
