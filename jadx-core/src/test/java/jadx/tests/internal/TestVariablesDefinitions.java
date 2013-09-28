package jadx.tests.internal;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.visitors.DepthTraverser;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.utils.exceptions.DecodeException;

import java.util.List;

import org.slf4j.Logger;

public class TestVariablesDefinitions extends InternalJadxTest {

	public static class TestCls {
		private static Logger LOG;
		private ClassNode cls;
		private List<IDexTreeVisitor> passes;

		public void run() {
			try {
				cls.load();
				for (IDexTreeVisitor visitor : passes) {
					DepthTraverser.visit(visitor, cls);
				}
			} catch (DecodeException e) {
				LOG.error("Decode exception: " + cls, e);
			} finally {
				cls.unload();
			}
		}
	}

	//@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);

		String code = cls.getCode().toString();

		System.out.println(code);
	}
}
