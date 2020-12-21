package jadx.tests.integration.rename;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestRenameEnum extends IntegrationTest {

	public static class TestCls {

		public enum A implements Runnable {
			ONE {
				@Override
				public void run() {
					System.out.println("ONE");
				}
			},
			TWO {
				@Override
				public void run() {
					System.out.println("TWO");
				}
			};
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		assertThat(cls).code()
				.containsOnlyOnce("public enum A ")
				.containsOnlyOnce("ONE {");

		cls.getInnerClasses().get(0).getClassInfo().changeShortName("ARenamed");

		assertThat(cls).reloadCode(this)
				.containsOnlyOnce("public enum ARenamed ")
				.containsOnlyOnce("ONE {");
	}
}
