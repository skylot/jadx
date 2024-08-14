package jadx.tests.integration.loops;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

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
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("for (Authorization auth : service.getAuthorizations()) {")
				.containsOne("if (isValid(auth)) {")
				.containsOne("return auth.getToken();");
	}
}
