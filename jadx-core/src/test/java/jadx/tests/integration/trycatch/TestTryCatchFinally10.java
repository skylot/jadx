package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTryCatchFinally10 extends SmaliTest {

	// @formatter:off
	/*
		public static String test(Context context, int i) {
			CommonContracts.requireNonNull(context);
			InputStream inputStream = null;
			try {
				inputStream = context.getResources().openRawResource(i);
				Scanner useDelimiter = new Scanner(inputStream).useDelimiter("\\A");
				return useDelimiter.hasNext() ? useDelimiter.next() : "";
			} finally {
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (IOException e) {
						l.logException(LogLevel.ERROR, e);
					}
				}
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		disableCompilation();
		assertThat(getClassNodeFromSmali())
				.code()
				.doesNotContain("boolean z = null;")
				.doesNotContain("} catch (Throwable")
				.containsOne("} finally {")
				.containsOne(".close();")
				.containsOne("} catch (IOException e")
				.containsOne(".logException(");
	}
}
