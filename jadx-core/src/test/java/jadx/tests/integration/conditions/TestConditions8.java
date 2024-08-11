package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestConditions8 extends IntegrationTest {

	public static class TestCls {
		private TestCls pager;
		private TestCls listView;

		public void test(TestCls view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			if (!isUsable()) {
				return;
			}
			if (!pager.hasMore()) {
				return;
			}
			if (getLoaderManager().hasRunningLoaders()) {
				return;
			}
			if (listView != null
					&& listView.getLastVisiblePosition() >= pager.size()) {
				showMore();
			}
		}

		private void showMore() {
		}

		private int size() {
			return 0;
		}

		private int getLastVisiblePosition() {
			return 0;
		}

		private boolean hasRunningLoaders() {
			return false;
		}

		private TestCls getLoaderManager() {
			return null;
		}

		private boolean hasMore() {
			return false;
		}

		private boolean isUsable() {
			return false;
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("showMore();");
	}
}
