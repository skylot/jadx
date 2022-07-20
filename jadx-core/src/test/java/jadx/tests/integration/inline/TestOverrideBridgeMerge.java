package jadx.tests.integration.inline;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.annotations.NodeDeclareRef;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestOverrideBridgeMerge extends SmaliTest {

	public static class TestCls implements Function<String, Integer> {
		@Override
		public /* bridge */ /* synthetic */ Integer apply(String str) {
			return test(str);
		}

		public Integer test(String str) {
			return str.length();
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("Integer test(String str) {"); // not inlined
	}

	@Test
	public void testSmali() {
		ClassNode cls = getClassNodeFromSmali();
		ICodeAnnotation mthDef = new NodeDeclareRef(getMethod(cls, "apply"));
		assertThat(cls)
				.checkCodeAnnotationFor("apply(String str) {", mthDef)
				.code()
				.containsOne("@Override")
				.containsOne("public Integer apply(String str) {")
				.doesNotContain("test(String str)");
	}
}
