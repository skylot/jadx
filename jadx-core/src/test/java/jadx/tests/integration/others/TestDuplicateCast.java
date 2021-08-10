package jadx.tests.integration.others;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.BlockUtils;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
		ClassNode cls = getClassNode(TestCls.class);
		MethodNode mth = getMethod(cls, "method");

		String code = cls.getCode().toString();
		assertThat(code, containsString("return (int[]) o;"));

		List<InsnNode> insns = BlockUtils.collectAllInsns(mth.getBasicBlocks());
		assertThat(insns, hasSize(1));
		InsnNode insnNode = insns.get(0);
		assertThat(insnNode.getType(), is(InsnType.RETURN));
		assertTrue(insnNode.getArg(0).isInsnWrap());
		InsnNode wrapInsn = ((InsnWrapArg) insnNode.getArg(0)).getWrapInsn();
		assertThat(wrapInsn.getType(), is(InsnType.CHECK_CAST));
		assertFalse(wrapInsn.getArg(0).isInsnWrap());
	}
}
