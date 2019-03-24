package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class TestAnonymousClass6 extends IntegrationTest {

	public static class TestCls {
		public Runnable test(final double d) {
			return new Runnable() {
				public void run() {
					System.out.println(d);
				}
			};
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("public Runnable test(final double d) {"));
		assertThat(code, containsOne("return new Runnable() {"));
		assertThat(code, containsOne("public void run() {"));
		assertThat(code, containsOne("System.out.println(d);"));
		assertThat(code, not(containsString("synthetic")));
	}
}
