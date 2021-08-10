package jadx.tests.integration.variables;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestVariablesGeneric extends SmaliTest {
	// @formatter:off
	/*
		public static <T> j a(i<? super T> iVar, c<T> cVar) {
			if (iVar == null) {
				throw new IllegalArgumentException("subscriber can not be null");
			}
			if (cVar.a == null) {
				throw new IllegalStateException("onSubscribe function can not be null.");
			}
			...
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		disableCompilation();
		assertThat(getClassNodeFromSmali())
				.code()
				.doesNotContain("iVar2")
				.containsOne("public static <T> j a(i<? super T> iVar, c<T> cVar) {")
				.containsOne("if (iVar == null) {")
				.countString(2, "} catch (Throwable th");
	}
}
