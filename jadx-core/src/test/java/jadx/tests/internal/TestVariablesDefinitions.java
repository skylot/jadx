package jadx.tests.internal;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.visitors.DepthTraversal;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.utils.exceptions.DecodeException;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestVariablesDefinitions extends InternalJadxTest {

	public static class TestCls {
		private static Logger LOG;
		private ClassNode cls;
		private List<IDexTreeVisitor> passes;

		public void run() {
			try {
				cls.load();
				Iterator<IDexTreeVisitor> iterator = passes.iterator();
				while (iterator.hasNext()) {
					DepthTraversal.visit(iterator.next(), cls);
				}
			} catch (DecodeException e) {
				LOG.error("Decode exception: " + cls, e);
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		// 'iterator' variable must be declared inside 'try' block
		assertThat(code, containsString(indent(3) + "Iterator<IDexTreeVisitor> iterator = "));
		assertThat(code, not(containsString("iterator;")));
	}
}
