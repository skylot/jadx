package jadx.tests.integration.variables;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class TestVariablesGeneric extends SmaliTest {
	/*
    public static <T> j a(i<? super T> iVar, c<T> cVar) {
        if (iVar == null) {
            throw new IllegalArgumentException("subscriber can not be null");
        }
        if (cVar.a == null) {
            throw new IllegalStateException("onSubscribe function can not be null.");
        }
        ...
	*/

	@Test
	public void test() {
		disableCompilation();
		ClassNode cls = getClassNodeFromSmaliWithPath("variables", "TestVariablesGeneric");
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("iVar2")));
		assertThat(code, containsString("public static <T> j a(i<? super T> iVar, c<T> cVar) {"));
		assertThat(code, containsString("if (iVar == null) {"));
	}
}
