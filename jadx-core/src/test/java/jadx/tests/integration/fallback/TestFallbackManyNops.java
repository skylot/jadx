package jadx.tests.integration.fallback;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestFallbackManyNops extends SmaliTest {

	@Test
	public void test() {
		setFallback();
		disableCompilation();

		ClassNode cls = getClassNodeFromSmali();
		String code = cls.getCode().toString();

		assertThat(code, containsString("public static void test() {"));
		assertThat(code, containsOne("return"));
		assertThat(code, not(containsString("Method dump skipped")));
	}
}
