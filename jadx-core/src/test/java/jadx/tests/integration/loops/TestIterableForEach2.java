package jadx.tests.integration.loops;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestIterableForEach2 extends IntegrationTest {

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

		assertThat(code, containsOne("for (Authorization auth : service.getAuthorizations()) {"));
		assertThat(code, containsOne("if (isValid(auth)) {"));
		assertThat(code, containsOne("return auth.getToken();"));
	}
}
