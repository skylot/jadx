package jadx.tests.integration.conditions;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestConditions13 extends IntegrationTest {

	public static class TestCls {
		static boolean qualityReading;

		public static void dataProcess(int raw, int quality) {
			if (quality >= 10 && raw != 0) {
				System.out.println("OK" + raw);
				qualityReading = false;
			} else if (raw == 0 || quality < 6 || !qualityReading) {
				System.out.println("Not OK" + raw);
			} else {
				System.out.println("Quit OK" + raw);
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("if (quality >= 10 && raw != 0) {"));
		assertThat(code, containsOne("System.out.println(\"OK\" + raw);"));
		assertThat(code, containsOne("qualityReading = false;"));
		assertThat(code, containsOne("} else if (raw == 0 || quality < 6 || !qualityReading) {"));
		assertThat(code, not(containsString("return")));

	}
}
