package jadx.core.dex.visitors.blocksmaker;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.NamedArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.trycatch.CatchAttr;
import jadx.core.dex.trycatch.ExcHandlerAttr;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.trycatch.TryCatchBlock;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InsnRemover;

public class BlockExceptionHandler extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}
		for (BlockNode block : mth.getBasicBlocks()) {
			markExceptionHandlers(block);
		}
		for (BlockNode block : mth.getBasicBlocks()) {
			block.updateCleanSuccessors();
		}
		for (BlockNode block : mth.getBasicBlocks()) {
			processExceptionHandlers(mth, block);
		}
		for (BlockNode block : mth.getBasicBlocks()) {
			processTryCatchBlocks(block);
		}
	}

	/**
	 * Set exception handler attribute for whole block
	 */
	private static void markExceptionHandlers(BlockNode block) {
		ExcHandlerAttr handlerAttr = block.get(AType.EXC_HANDLER);
		if (handlerAttr == null) {
			return;
		}
		ExceptionHandler excHandler = handlerAttr.getHandler();
		ArgType argType = excHandler.getArgType();
		InsnNode me = BlockUtils.getLastInsn(block);
		if (me != null && me.getType() == InsnType.MOVE_EXCEPTION) {
			// set correct type for 'move-exception' operation
			RegisterArg resArg = InsnArg.reg(me.getResult().getRegNum(), argType);
			resArg.copyAttributesFrom(me);
			me.setResult(resArg);
			me.add(AFlag.DONT_INLINE);
			resArg.add(AFlag.CUSTOM_DECLARE);
			excHandler.setArg(resArg);
			me.addAttr(handlerAttr);
			return;
		}
		// handler arguments not used
		excHandler.setArg(new NamedArg("unused", argType));
	}

	private static void processExceptionHandlers(MethodNode mth, BlockNode block) {
		ExcHandlerAttr handlerAttr = block.get(AType.EXC_HANDLER);
		if (handlerAttr == null) {
			return;
		}
		ExceptionHandler excHandler = handlerAttr.getHandler();
		excHandler.addBlock(block);
		for (BlockNode node : BlockUtils.collectBlocksDominatedBy(block, block)) {
			excHandler.addBlock(node);
		}
		for (BlockNode excBlock : excHandler.getBlocks()) {
			// remove 'monitor-exit' from exception handler blocks
			InsnRemover remover = new InsnRemover(mth, excBlock);
			for (InsnNode insn : excBlock.getInstructions()) {
				if (insn.getType() == InsnType.MONITOR_ENTER) {
					break;
				}
				if (insn.getType() == InsnType.MONITOR_EXIT) {
					remover.addAndUnbind(insn);
				}
			}
			remover.perform();

			// if 'throw' in exception handler block have 'catch' - merge these catch blocks
			for (InsnNode insn : excBlock.getInstructions()) {
				CatchAttr catchAttr = insn.get(AType.CATCH_BLOCK);
				if (catchAttr == null) {
					continue;
				}
				if (insn.getType() == InsnType.THROW
						|| onlyAllHandler(catchAttr.getTryBlock())) {
					TryCatchBlock handlerBlock = handlerAttr.getTryBlock();
					TryCatchBlock catchBlock = catchAttr.getTryBlock();
					handlerBlock.merge(mth, catchBlock);
				}
			}
		}
	}

	private static boolean onlyAllHandler(TryCatchBlock tryBlock) {
		if (tryBlock.getHandlersCount() == 1) {
			ExceptionHandler eh = tryBlock.getHandlers().iterator().next();
			return eh.isCatchAll() || eh.isFinally();
		}
		return false;
	}

	/**
	 * If all instructions in block have same 'catch' attribute mark it as 'TryCatch' block.
	 */
	private static void processTryCatchBlocks(BlockNode block) {
		CatchAttr commonCatchAttr = null;
		for (InsnNode insn : block.getInstructions()) {
			CatchAttr catchAttr = insn.get(AType.CATCH_BLOCK);
			if (catchAttr == null) {
				continue;
			}
			if (commonCatchAttr == null) {
				commonCatchAttr = catchAttr;
			} else if (commonCatchAttr != catchAttr) {
				commonCatchAttr = null;
				break;
			}
		}
		if (commonCatchAttr != null) {
			block.addAttr(commonCatchAttr);
		}
	}
}
