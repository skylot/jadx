package jadx.tests.integration.inner;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class TestAnonymousClass8 extends IntegrationTest {

	public static class TestCls {

		public final double d = Math.abs(4);

		public Runnable test() {
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

		assertThat(code, containsOne("public Runnable test() {"));
		assertThat(code, containsOne("return new Runnable() {"));
		assertThat(code, containsOne("public void run() {"));
		assertThat(code, containsOne("this.d);"));
		assertThat(code, not(containsString("synthetic")));
	}
}
