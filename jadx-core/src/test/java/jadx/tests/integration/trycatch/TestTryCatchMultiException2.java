package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

@SuppressWarnings("CommentedOutCode")
public class TestTryCatchMultiException2 extends SmaliTest {

	// @formatter:off
	/*
    public static boolean test() {
        try {
            Class<?> cls = Class.forName("c");
            return ((Boolean) cls.getMethod("b", new Class[0]).invoke(cls, new Object[0])).booleanValue();
        } catch (ClassNotFoundException | NoSuchMethodException | Exception | Throwable unused) {
        	// java compiler don't allow shadow subclasses in multi-catch
        	// in this case leave only Throwable
            return false;
        }
    }
	*/
	// @formatter:on

	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("} catch (Throwable unused) {");
	}
}
