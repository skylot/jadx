package jadx.tests.integration.inner;

import java.io.File;
import java.io.FilenameFilter;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

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
		assertThat(getClassNode(TestCls.class))
				.code()
				.contains("new File(\"a\").list(new FilenameFilter()")
				.doesNotContain("synthetic")
				.doesNotContain("this")
				.doesNotContain("null")
				.doesNotContain("AnonymousClass_")
				.doesNotContain("class AnonymousClass");
	}

	@Test
	public void testNoInline() {
		getArgs().setInlineAnonymousClasses(false);
		assertThat(getClassNode(TestCls.class))
				.code()
				.contains("class AnonymousClass1 implements FilenameFilter {")
				.containsOne("new AnonymousClass1()");
	}
}
