package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestConditions12 extends IntegrationTest {

	public static class TestCls {
		static boolean autoStop = true;
		static boolean qualityReading = false;
		static int lastValidRaw = -1;

		public static void main(String[] args) throws Exception {
			int a = 5;
			int b = 30;
			dataProcess(a, b);
		}

		public static void dataProcess(int raw, int quality) {
			if (quality >= 10 && raw != 0) {
				System.out.println("OK" + raw);
				qualityReading = false;
			} else if (raw == 0 || quality < 6 || !qualityReading) {
				System.out.println("Not OK" + raw);
			} else {
				System.out.println("Quit OK" + raw);
			}
			if (quality < 30) {
				int timeLeft = 30 - quality;
				if (quality >= 10) {
					System.out.println("Processing" + timeLeft);
				}
			} else {
				System.out.println("Finish Processing");
				if (raw > 0) {
					lastValidRaw = raw;
				}
			}
			if (quality >= 30 && autoStop) {
				System.out.println("Finished");
			}
			if (!autoStop && lastValidRaw > -1 && quality < 10) {
				System.out.println("Finished");
			}
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("if (quality >= 10 && raw != 0) {")
				.containsOne("} else if (raw == 0 || quality < 6 || !qualityReading) {")
				.containsOne("if (quality < 30) {")
				.containsOne("if (quality >= 10) {")
				.containsOne("if (raw > 0) {")
				.containsOne("if (quality >= 30 && autoStop) {")
				.containsOne("if (!autoStop && lastValidRaw > -1 && quality < 10) {")
				.doesNotContain("return");
	}
}
