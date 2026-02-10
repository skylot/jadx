package jadx.tests.integration.trycatch;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

// Ensure that we can still merge if conditions even in the case where
// the BOTTOM_SPLITTER occurs before the final IF block.
public class TestTryCatch11 extends IntegrationTest {

	public static class TestCls {
		public static class Cursor implements AutoCloseable {
			@Override
			public void close() {
				System.out.println("Closed AutoCloseableResources_First");
			}

			public String getString() {
				return "jfdkelapgfureiqop[]";
			}
		}

		public static String test() {
			try (Cursor cursor = new Cursor()) {
				String value = cursor.getString();
				if (value.startsWith("content://") || !value.startsWith("/") && !value.startsWith("file://")) {
					return null;
				}
				return value;
			} catch (Exception ignore) {
				System.out.println("catch");
			}
			return null;
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.JAVA8 })
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("value.startsWith(\"content://\") || (!value.startsWith(\"/\") && !value.startsWith(\"file://\"))");
	}
}
