package jadx.tests.integration.conditions;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("showMore();"));
	}
}
