package jadx.tests.integration;

import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.tests.api.IntegrationTest;

import java.util.List;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test duplicate 'check-cast' instruction produced because of bug in javac:
 * http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6246854
 */
public class TestDuplicateCast extends IntegrationTest {

	public static class TestCls {
		public int[] method(Object o) {
			return (int[]) o;
		}
	}

	@Test
	public void test() {
		dontUnloadClass();
		ClassNode cls = getClassNode(TestCls.class);
		MethodNode mth = getMethod(cls, "method");

		String code = cls.getCode().toString();
		assertThat(code, containsString("return (int[]) o;"));

		List<InsnNode> insns = mth.getBasicBlocks().get(1).getInstructions();
		assertEquals(insns.size(), 1);
		InsnNode insnNode = insns.get(0);
		assertEquals(InsnType.RETURN, insnNode.getType());
		assertTrue(insnNode.getArg(0).isInsnWrap());
		InsnNode wrapInsn = ((InsnWrapArg) insnNode.getArg(0)).getWrapInsn();
		assertEquals(InsnType.CHECK_CAST, wrapInsn.getType());
		assertFalse(wrapInsn.getArg(0).isInsnWrap());
	}
}
