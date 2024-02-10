package jadx.tests.integration.types;

import java.io.Closeable;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestInterfacesCast extends IntegrationTest {

	public static class TestCls {

		public Runnable test(Closeable obj) throws IOException {
			return (Runnable) obj;
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return (Runnable) closeable;");
	}
}
