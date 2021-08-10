package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Issue 1197
 */
public class TestTypeResolver17 extends SmaliTest {
	// @formatter:off
	/*
		private static String test(Context context, Uri uri, String str, String str2) {
			Cursor cursor = null;
			try {
				cursor = context.getContentResolver().query(uri, new String[]{str}, null, null, null);
				if (cursor.moveToFirst() && !cursor.isNull(0)) {
					return cursor.getString(0);
				}
				closeQuietly(cursor);
				return str2;
			} catch (Exception e) {
				Log.w("DocumentFile", "Failed query: " + e);
				return str2;
			} finally {
				closeQuietly(cursor);
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		disableCompilation();
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("Cursor cursor = null;")
				.doesNotContain("(AutoCloseable autoCloseable = ");
	}
}
