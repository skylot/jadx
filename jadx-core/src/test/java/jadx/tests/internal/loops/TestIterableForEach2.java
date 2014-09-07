package jadx.tests.internal.loops;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import static jadx.tests.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestIterableForEach2 extends InternalJadxTest {

	public static class TestCls {
		public static String test(final Service service) throws IOException {
			for (Authorization auth : service.getAuthorizations()) {
				if (isValid(auth)) {
					return auth.getToken();
				}
			}
			return null;
		}

		private static boolean isValid(Authorization auth) {
			return false;
		}

		private static class Service {
			public List<Authorization> getAuthorizations() {
				return null;
			}
		}

		private static class Authorization {
			public String getToken() {
				return "";
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, containsOne("for (Authorization auth : service.getAuthorizations()) {"));
		assertThat(code, containsOne("if (isValid(auth)) {"));
		assertThat(code, containsOne("return auth.getToken();"));
	}
}
