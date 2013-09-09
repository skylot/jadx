package jadx.api;

import jadx.core.dex.instructions.ArithNode;
import jadx.core.dex.instructions.ArithOp;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;

import java.util.List;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestFieldIncrement extends InternalJadxTest {

	public static class TestCls {
		public int field = 1;

		public void method() {
			field++;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		MethodNode mth = getMethod(cls, "method");

		List<InsnNode> insns = mth.getBasicBlocks().get(0).getInstructions();
		assertEquals(insns.size(), 1);
		InsnNode insnNode = insns.get(0);
		assertEquals(InsnType.ARITH, insnNode.getType());
		assertEquals(ArithOp.ADD, ((ArithNode) insnNode).getOp());

		assertThat(cls.getCode().toString(), containsString("field++;"));
	}
}
