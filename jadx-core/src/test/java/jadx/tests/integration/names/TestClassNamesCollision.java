package jadx.tests.integration.names;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.api.CommentsLevel;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;
import jadx.tests.integration.names.pkg.a;
import jadx.tests.integration.names.pkg.b;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestClassNamesCollision extends IntegrationTest {

	@Test
	public void test() {
		getArgs().setCommentsLevel(CommentsLevel.WARN);
		List<ClassNode> classNodes = getClassNodes(a.class, b.class);

		assertThat(searchCls(classNodes, "a"))
				.code()
				.containsOne("public class a {")
				.containsOne("public static a a() {");

		assertThat(searchCls(classNodes, "b"))
				.code()
				.containsOne("class a {")
				.containsOne("jadx.tests.integration.names.pkg.a a = jadx.tests.integration.names.pkg.a.a();");
	}
}
