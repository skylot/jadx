package jadx.tests.integration.trycatch;

import java.util.List;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.Utils;
import jadx.tests.api.IntegrationTest;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;
import static org.junit.Assert.assertThat;

public class TestTryCatch2 extends IntegrationTest {

	public static class TestCls {
		private final static Object obj = new Object();

		private static boolean test() {
			try {
				synchronized (obj) {
					obj.wait(5);
				}
				return true;
			} catch (InterruptedException e) {
				return false;
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("try {"));
		assertThat(code, containsString("synchronized (obj) {"));
		assertThat(code, containsString("obj.wait(5);"));
		assertThat(code, containsString("return true;"));
		assertThat(code, containsString("} catch (InterruptedException e) {"));
		assertThat(code, containsString("return false;"));
		
		//RAF test that remove of ExceptionHandler from Method handlers list, when
		// the handler has been moved into a TryCatchRegion worked (ie no duplicate
		// ref to handler blocks;
		MethodNode testMth = null;
		for (MethodNode mth : cls.getMethods()) {
			if (mth.getName().compareTo("test") == 0) {
				testMth = mth;
			}
		}
		String tree = Utils.genTree(testMth, "test method", 0);
		// There should be 2 occurs of 'ExcHandler' in the tree, not 4.  Four would
		// indicate that there were two copies of the exec handler region in the tree
		int cnt=0, prev=0, ind=0;
		while (true) {  // Count up number of occurances of 'ExcHandler' string
			if ((ind = tree.indexOf("ExcHandler", prev)) == -1) break;
			cnt++;	prev = ind+1;
		}
		assertEquals("Expected 2 occurs of 'ExcHandler' in tree, 4 means duplicate try/catch handler block",
		  cnt, 2);
	}
}
