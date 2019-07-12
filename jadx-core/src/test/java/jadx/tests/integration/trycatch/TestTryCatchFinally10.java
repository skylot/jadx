package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

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
		ClassNode cls = getClassNodeFromSmali();
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("boolean z = null;")));
		assertThat(code, not(containsString("} catch (Throwable")));
		assertThat(code, containsOne("} finally {"));
		assertThat(code, containsOne(".close();"));
	}
}
