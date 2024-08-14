package jadx.tests.integration.others;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.tests.api.IntegrationTest;

import static jadx.core.dex.instructions.InsnType.CHECK_CAST;
import static jadx.core.dex.instructions.InsnType.RETURN;
import static jadx.core.utils.BlockUtils.collectAllInsns;
import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

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

		assertThat(cls)
				.code()
				.contains("return (int[]) o;");

		List<InsnNode> insns = collectAllInsns(mth.getBasicBlocks());
		assertThat(insns).hasSize(1);
		InsnNode insnNode = insns.get(0);
		assertThat(insnNode.getType()).isEqualTo(RETURN);
		assertThat(insnNode.getArg(0).isInsnWrap()).isTrue();
		InsnNode wrapInsn = ((InsnWrapArg) insnNode.getArg(0)).getWrapInsn();
		assertThat(wrapInsn.getType()).isEqualTo(CHECK_CAST);
		assertThat(wrapInsn.getArg(0).isInsnWrap()).isFalse();
	}
}
