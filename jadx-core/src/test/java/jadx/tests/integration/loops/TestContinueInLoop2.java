package jadx.tests.integration.loops;

import org.junit.Test;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.trycatch.CatchAttr;
import jadx.core.dex.trycatch.ExcHandlerAttr;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.trycatch.TryCatchBlock;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InstructionRemover;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestContinueInLoop2 extends IntegrationTest {

	public static class TestCls {
		private static void test(MethodNode mth, BlockNode block) {
			ExcHandlerAttr handlerAttr = block.get(AType.EXC_HANDLER);
			if (handlerAttr != null) {
				ExceptionHandler excHandler = handlerAttr.getHandler();
				excHandler.addBlock(block);
				for (BlockNode node : BlockUtils.collectBlocksDominatedBy(block, block)) {
					excHandler.addBlock(node);
				}
				for (BlockNode excBlock : excHandler.getBlocks()) {
					InstructionRemover remover = new InstructionRemover(mth, excBlock);
					for (InsnNode insn : excBlock.getInstructions()) {
						if (insn.getType() == InsnType.MONITOR_ENTER) {
							break;
						}
						if (insn.getType() == InsnType.MONITOR_EXIT) {
							remover.add(insn);
						}
					}
					remover.perform();

					for (InsnNode insn : excBlock.getInstructions()) {
						if (insn.getType() == InsnType.THROW) {
							CatchAttr catchAttr = insn.get(AType.CATCH_BLOCK);
							if (catchAttr != null) {
								TryCatchBlock handlerBlock = handlerAttr.getTryBlock();
								TryCatchBlock catchBlock = catchAttr.getTryBlock();
								if (handlerBlock != catchBlock) {
									handlerBlock.merge(mth, catchBlock);
									catchBlock.removeInsn(mth, insn);
								}
							}
						}
					}
				}
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("break;"));
		assertThat(code, not(containsString("continue;")));
	}
}
