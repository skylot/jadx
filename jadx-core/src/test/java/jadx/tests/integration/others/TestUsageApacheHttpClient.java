package jadx.tests.integration.others;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

public class TestUsageApacheHttpClient extends SmaliTest {

	// @formatter:off
	/*
		package others;
		import org.apache.http.client.HttpClient;

		public class HttpClientTest {
			private HttpClient httpClient;
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		disableCompilation();
		ClassNode cls = getClassNodeFromSmali();
		Assertions.assertTrue(cls.root().getGradleInfoStorage().isUseApacheHttpLegacy());
	}
}
