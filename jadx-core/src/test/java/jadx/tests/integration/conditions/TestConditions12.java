package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("if (quality >= 10 && raw != 0) {"));
		assertThat(code, containsOne("} else if (raw == 0 || quality < 6 || !qualityReading) {"));
		assertThat(code, containsOne("if (quality < 30) {"));
		assertThat(code, containsOne("if (quality >= 10) {"));
		assertThat(code, containsOne("if (raw > 0) {"));
		assertThat(code, containsOne("if (quality >= 30 && autoStop) {"));
		assertThat(code, containsOne("if (!autoStop && lastValidRaw > -1 && quality < 10) {"));
		assertThat(code, not(containsString("return")));
	}
}
