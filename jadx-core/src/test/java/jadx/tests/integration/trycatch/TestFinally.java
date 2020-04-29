package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestFinally extends IntegrationTest {

	public static class TestCls {
		private static final String DISPLAY_NAME = "name";

		String test(Context context, Object uri) {
			Cursor cursor = null;
			try {
				String[] projection = { DISPLAY_NAME };
				cursor = context.query(uri, projection);
				int columnIndex = cursor.getColumnIndexOrThrow(DISPLAY_NAME);
				cursor.moveToFirst();
				return cursor.getString(columnIndex);
			} finally {
				if (cursor != null) {
					cursor.close();
				}
			}
		}

		private class Context {
			public Cursor query(Object o, String[] s) {
				return null;
			}
		}

		private class Cursor {
			public void close() {
			}

			public void moveToFirst() {
			}

			public int getColumnIndexOrThrow(String s) {
				return 0;
			}

			public String getString(int i) {
				return null;
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("} finally {"));
		assertThat(code, containsOne("cursor.getString(columnIndex);"));
		assertThat(code, not(containsOne("String str = true;")));
	}
}
