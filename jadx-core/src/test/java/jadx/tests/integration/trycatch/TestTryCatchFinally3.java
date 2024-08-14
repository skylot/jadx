package jadx.tests.integration.trycatch;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.visitors.DepthTraversal;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestTryCatchFinally3 extends IntegrationTest {

	public static class TestCls {
		private static final Logger LOG = LoggerFactory.getLogger(TestCls.class);

		public static void test(ClassNode cls, List<IDexTreeVisitor> passes) {
			try {
				cls.load();
				for (IDexTreeVisitor visitor : passes) {
					DepthTraversal.visit(visitor, cls);
				}
			} catch (Exception e) {
				LOG.error("Class process exception: {}", cls, e);
			} finally {
				cls.unload();
			}
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("for (IDexTreeVisitor visitor : passes) {")
				.containsOne("} catch (Exception e) {")
				.containsOne("LOG.error(\"Class process exception: {}\", cls, e);")
				.containsOne("} finally {")
				.containsOne("cls.unload();");
	}
}
