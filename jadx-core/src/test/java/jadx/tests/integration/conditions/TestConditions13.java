package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

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
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("if (quality >= 10 && raw != 0) {")
				.containsOne("System.out.println(\"OK\" + raw);")
				.containsOne("qualityReading = false;")
				.containsOne("} else if (raw == 0 || quality < 6 || !qualityReading) {")
				.doesNotContain("return");
	}
}
