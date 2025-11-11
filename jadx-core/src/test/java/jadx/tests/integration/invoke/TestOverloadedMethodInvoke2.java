package jadx.tests.integration.invoke;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestOverloadedMethodInvoke2 extends IntegrationTest {

	public static class AbstractItem {

		public void doSomething(Container c, Item i) {
			c.add(i);
		}

		public static class Container {

			public <T extends AbstractItem> int add(T t) {
				return 0;
			}

			public void add(AbstractItem... item) {
			}
		}

		public static class Item extends AbstractItem {
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestOverloadedMethodInvoke2.AbstractItem.class))
				.code().containsOne("c.add(i);")
				.doesNotContain("(Container)");
	}
}
