package jadx.tests.integration.trycatch;

import java.io.FileInputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestEmptyFinally extends IntegrationTest {

	public static class TestCls {
		@SuppressWarnings("EmptyFinallyBlock")
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

	@NotYetImplemented
	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("} catch (IOException e) {"));
		assertThat(code, containsOne("} finally {")); // ???
	}
}
