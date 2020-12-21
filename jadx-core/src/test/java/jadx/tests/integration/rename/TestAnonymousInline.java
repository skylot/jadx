package jadx.tests.integration.rename;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestAnonymousInline extends IntegrationTest {

	public static class TestCls {
		public Runnable test() {
			return new Runnable() {
				@Override
				public void run() {
					System.out.println("run");
				}
			};
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		assertThat(cls).code()
				.containsOnlyOnce("return new Runnable() {");

		assertThat(cls).reloadCode(this)
				.removeBlockComments() // remove comment about inlined class
				.containsOnlyOnce("return new Runnable() {")
				.doesNotContain("AnonymousClass1");
	}
}
