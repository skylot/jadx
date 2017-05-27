package jadx.tests.integration.trycatch;

import java.io.IOException;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestTryCatch6 extends IntegrationTest {

	public static class TestCls {
		private static boolean test(Object obj) {
			boolean res = false;
			while (true) {
				try {
					res = exc(obj);
					return res;
				} catch (IOException e) {
					res = true;
				} catch (Throwable e) {
					if (obj == null) {
						obj = new Object();
					}
				}
			}
		}

		private static boolean exc(Object obj) throws IOException {
			if (obj == null) {
				throw new IOException();
			}
			return true;
		}

		public void check() {
			assertTrue(test(new Object()));
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("try {"));
	}
}
