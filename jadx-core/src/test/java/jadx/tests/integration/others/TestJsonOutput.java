package jadx.tests.integration.others;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.api.JadxArgs;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

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
		args.setOutputFormat(JadxArgs.OutputFormatEnum.JSON);

		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("\"offset\": \"0x"));
		assertThat(code, containsOne("public static class Inner implements Runnable"));
	}

	@Test
	public void testFallback() {
		disableCompilation();
		setFallback();
		args.setOutputFormat(JadxArgs.OutputFormatEnum.JSON);

		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("\"offset\": \"0x"));
		assertThat(code, containsOne("public static class Inner implements java.lang.Runnable"));
	}
}
