package jadx.tests.integration.inline;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

@SuppressWarnings("CommentedOutCode")
public class TestOverlapSyntheticMethods extends SmaliTest {
	// @formatter:off
	/*
		public String test(int i) {
			return a(i) + "|" + a(i);
		}

		public int a(int i) {
			return i;
		}

		public String a(int i) {
			return "i:" + i;
		}
	*/
	// @formatter:on

	@Test
	public void testSmali() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("int a(int i) {")
				.containsOne("String m0a(int i) {");
	}

	@Test
	public void testSmaliNoRename() {
		getArgs().setRenameFlags(Collections.emptySet());
		disableCompilation();
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("int a(int i) {")
				.containsOne("String a(int i) {")
				.containsOne("return a(i) + \"|\" + a(i);");
	}
}
