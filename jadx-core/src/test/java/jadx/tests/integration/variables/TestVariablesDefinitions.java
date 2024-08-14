package jadx.tests.integration.variables;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.visitors.DepthTraversal;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestVariablesDefinitions extends IntegrationTest {

	public static class TestCls {
		private static Logger log;
		private ClassNode cls;
		private List<IDexTreeVisitor> passes;

		public void test() {
			try {
				cls.load();
				for (IDexTreeVisitor pass : this.passes) {
					DepthTraversal.visit(pass, cls);
				}
			} catch (Exception e) {
				log.error("Decode exception: {}", cls, e);
			}
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne(indent(3) + "for (IDexTreeVisitor pass : this.passes) {")
				.doesNotContain("iterator;")
				.doesNotContain("Iterator");
	}
}
