package jadx.tests.integration.others;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static jadx.api.JadxArgs.OutputFormatEnum.JSON;

public class TestJsonOutput extends IntegrationTest {

	public static class TestCls {
		private final String prefix = "list: ";

		static {
			System.out.println("test");
		}

		public void test(boolean b, List<String> list) {
			if (b) {
				System.out.println(prefix + list);
			}
		}

		public static class Inner implements Runnable {
			@Override
			public void run() {
				System.out.println("run");
			}
		}
	}

	@Test
	public void test() {
		disableCompilation();
		args.setOutputFormat(JSON);

		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("\"offset\": \"0x")
				.containsOne("public static class Inner implements Runnable");
	}

	@Test
	public void testFallback() {
		disableCompilation();
		setFallback();
		args.setOutputFormat(JSON);

		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("\"offset\": \"0x")
				.containsOne("public static class Inner implements java.lang.Runnable");
	}
}
