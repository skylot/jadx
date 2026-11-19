package jadx.tests.integration.generics;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestOverrideCollisionRename2 extends SmaliTest {

	@Test
	public void test() {
		List<ClassNode> classes = loadFromSmaliFiles();

		assertThat(searchCls(classes, "generics.ZMutable"))
				.code()
				.containsOne("T getValue();")
				.containsOne("void setValue(T t);")
				.doesNotContain("getValue2");

		assertThat(searchCls(classes, "generics.AMutableBoolean"))
				.code()
				.countString(2, "@Override")
				.containsOne("public Boolean getValue() {")
				.doesNotContain("getValue2");

		assertThat(searchCls(classes, "generics.NBoxImpl"))
				.code()
				.countString(2, "@Override")
				.containsOne("public MBox<String> getValue() {")
				.doesNotContain("getValue2");

		assertThat(searchCls(classes, "generics.ParentCollision"))
				.code()
				.containsOne("public MBox<String> call() {")
				.containsOne("public int call2() {");

		assertThat(searchCls(classes, "generics.AParentSubFirst"))
				.code()
				.containsOne("@Override")
				.containsOne("public int call2() {");

		assertThat(searchCls(classes, "generics.ZParentSubLast"))
				.code()
				.containsOne("@Override")
				.containsOne("public int call2() {");
	}
}
