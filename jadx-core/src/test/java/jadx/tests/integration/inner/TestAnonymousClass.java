package jadx.tests.integration.inner;

import java.io.File;
import java.io.FilenameFilter;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestAnonymousClass extends IntegrationTest {

	public static class TestCls {

		public int test() {
			String[] files = new File("a").list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.equals("a");
				}
			});
			return files.length;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("new File(\"a\").list(new FilenameFilter()"));
		assertThat(code, not(containsString("synthetic")));
		assertThat(code, not(containsString("this")));
		assertThat(code, not(containsString("null")));
		assertThat(code, not(containsString("AnonymousClass_")));
		assertThat(code, not(containsString("class AnonymousClass")));
	}

	@Test
	public void testNoInline() {
		getArgs().setInlineAnonymousClasses(false);

		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("class AnonymousClass1 implements FilenameFilter {"));
		assertThat(code, containsOne("new AnonymousClass1()"));
	}
}
