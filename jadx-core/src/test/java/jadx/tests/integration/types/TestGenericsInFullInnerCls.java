package jadx.tests.integration.types;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.api.CommentsLevel;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestGenericsInFullInnerCls extends SmaliTest {

	@Test
	public void test() {
		getArgs().setCommentsLevel(CommentsLevel.WARN);
		List<ClassNode> classNodes = loadFromSmaliFiles();

		assertThat(searchCls(classNodes, "types.FieldCls"))
				.code()
				.containsOne("private ba<n>.bb<n, n> a;");

		assertThat(searchCls(classNodes, "types.test.ba"))
				.code()
				.containsOne("public final class ba<S> {")
				.containsOne("public final class bb<T, V extends n> {")
				.containsOne("private ba<S> b;")
				.containsOne("private ba<S>.bb<T, V>.bc<T, V> c;")
				.containsOne("public final class bc<T, V extends n> {")
				.containsOne("private ba<S> a;");
	}

	@Test
	public void testWithDeobf() {
		enableDeobfuscation();
		args.setDeobfuscationMinLength(100); // rename everything

		getArgs().setCommentsLevel(CommentsLevel.WARN);
		loadFromSmaliFiles();
		// compilation should pass
	}

	@Test
	public void testWithFullNames() {
		getArgs().setUseImports(false);
		getArgs().setCommentsLevel(CommentsLevel.WARN);
		loadFromSmaliFiles();
		// compilation should pass
	}
}
