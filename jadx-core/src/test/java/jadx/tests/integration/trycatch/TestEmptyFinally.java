package jadx.tests.integration.trycatch;

import java.io.FileInputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestEmptyFinally extends IntegrationTest {

	@SuppressWarnings("EmptyFinallyBlock")
	public static class TestCls {
		public void test(FileInputStream f1) {
			try {
				f1.close();
			} catch (IOException e) {
				// do nothing
			} finally {
				// ignore
			}
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("} catch (IOException e) {")
				.doesNotContain("} finally {");
	}
}
