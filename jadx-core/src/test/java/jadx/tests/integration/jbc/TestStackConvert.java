package jadx.tests.integration.jbc;

import org.junit.jupiter.api.Test;

import jadx.tests.api.RaungTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestStackConvert extends RaungTest {

	@SuppressWarnings({ "UnnecessaryLocalVariable", "CallToPrintStackTrace", "printstacktrace" })
	public static class TestCls {
		public int parseIntDefault(String num, int defaultNum) {
			try {
				int defaultNum2 = Integer.parseInt(num);
				return defaultNum2;
			} catch (NumberFormatException e) {
				System.out.println("Before println");
				e.printStackTrace();
				return defaultNum;
			}
		}
	}

	@TestWithProfiles(TestProfile.JAVA11)
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("Integer.parseInt(num)");
	}

	@Test
	public void testRaung() {
		assertThat(getClassNodeFromRaung())
				.code()
				.containsOne("Integer.parseInt(num)");
	}
}
