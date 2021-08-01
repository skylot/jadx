package jadx.tests.integration.types;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestGenerics5 extends IntegrationTest {

	public static class TestCls {
		private InheritableThreadLocal<Map<String, String>> inheritableThreadLocal;

		public void test(String key, String val) {
			if (key == null) {
				throw new IllegalArgumentException("key cannot be null");
			}
			Map<String, String> map = this.inheritableThreadLocal.get();
			if (map == null) {
				map = new HashMap<>();
				this.inheritableThreadLocal.set(map);
			}
			map.put(key, val);
		}

		public void remove(String key) {
			Map<String, String> map = this.inheritableThreadLocal.get();
			if (map != null) {
				map.remove(key);
			}
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.countString(2, "Map<String, String> map = this.inheritableThreadLocal.get();");
	}
}
