package jadx.tests.integration.generics;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestConstructorGenerics extends IntegrationTest {

	@SuppressWarnings({ "MismatchedQueryAndUpdateOfCollection", "RedundantOperationOnEmptyContainer" })
	public static class TestCls {
		public String test() {
			Map<String, String> map = new HashMap<>();
			return map.get("test");
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("Map<String, String> map = new HashMap<>();");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return (String) new HashMap().get(\"test\");");
	}
}
