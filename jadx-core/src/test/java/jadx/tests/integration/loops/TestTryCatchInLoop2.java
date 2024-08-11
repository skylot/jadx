package jadx.tests.integration.loops;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestTryCatchInLoop2 extends IntegrationTest {

	public static class TestCls<T extends String> {
		private static class MyItem {
			int idx;
			String name;
		}

		private final Map<Integer, MyItem> mCache = new HashMap<>();

		void test(MyItem[] items) {
			synchronized (this.mCache) {
				for (int i = 0; i < items.length; ++i) {
					MyItem existingItem = mCache.get(items[i].idx);
					if (null == existingItem) {
						mCache.put(items[i].idx, items[i]);
					} else {
						existingItem.name = items[i].name;
					}
				}
			}
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("synchronized (this.mCache) {")
				.containsOne("for (int i = 0; i < items.length; i++) {");
	}
}
