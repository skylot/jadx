package jadx.core.dex.trycatch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.Utils;

public class TryCatchBlock {

	private final List<ExceptionHandler> handlers;

	// references for fast remove/modify
	private final List<InsnNode> insns;
	private final CatchAttr attr;

	public TryCatchBlock(int handlersCount) {
		handlers = new ArrayList<>(handlersCount);
		insns = new ArrayList<>();
		attr = new CatchAttr(this);
	}

	public Iterable<ExceptionHandler> getHandlers() {
		return handlers;
	}

	public int getHandlersCount() {
		return handlers.size();
	}

	public boolean containsAllHandlers(TryCatchBlock tb) {
		return handlers.containsAll(tb.handlers);
	}

	public ExceptionHandler addHandler(MethodNode mth, int addr, @Nullable ClassInfo type) {
		ExceptionHandler handler = new ExceptionHandler(addr, type);
		handler.setTryBlock(this);
		ExceptionHandler addedHandler = mth.addExceptionHandler(handler);
		if (addedHandler == handler || addedHandler.getTryBlock() != this) {
			handlers.add(addedHandler);
		}
		return addedHandler;
	}

	/**
	 * Use only before BlockSplitter
	 */
	public void removeSameHandlers(TryCatchBlock outerTry) {
		for (ExceptionHandler handler : outerTry.getHandlers()) {
			if (handlers.remove(handler)) {
				handler.setTryBlock(outerTry);
			}
		}
	}

	public void removeHandler(MethodNode mth, ExceptionHandler handler) {
		for (Iterator<ExceptionHandler> it = handlers.iterator(); it.hasNext();) {
			ExceptionHandler h = it.next();
			if (h == handler) {
				unbindHandler(h);
				it.remove();
				break;
			}
		}
		if (handlers.isEmpty()) {
			removeWholeBlock(mth);
		}
	}

	private void unbindHandler(ExceptionHandler handler) {
		for (BlockNode block : handler.getBlocks()) {
			// skip synthetic loop exit blocks
			BlockUtils.skipPredSyntheticPaths(block);
			block.add(AFlag.REMOVE);
			ExcHandlerAttr excHandlerAttr = block.get(AType.EXC_HANDLER);
			if (excHandlerAttr != null
					&& excHandlerAttr.getHandler().equals(handler)) {
				block.remove(AType.EXC_HANDLER);
			}
			SplitterBlockAttr splitter = handler.getHandlerBlock().get(AType.SPLITTER_BLOCK);
			if (splitter != null) {
				BlockNode splitterBlock = splitter.getBlock();
				splitterBlock.remove(AType.SPLITTER_BLOCK);
				for (BlockNode successor : splitterBlock.getSuccessors()) {
					successor.remove(AType.SPLITTER_BLOCK);
				}
			}
		}
		handler.markForRemove();
	}

	private void removeWholeBlock(MethodNode mth) {
		// self destruction
		for (Iterator<ExceptionHandler> it = handlers.iterator(); it.hasNext();) {
			ExceptionHandler h = it.next();
			unbindHandler(h);
			it.remove();
		}
		for (InsnNode insn : insns) {
			insn.removeAttr(attr);
		}
		insns.clear();
		if (mth.getBasicBlocks() != null) {
			for (BlockNode block : mth.getBasicBlocks()) {
				block.removeAttr(attr);
			}
		}
	}

	public void addInsn(InsnNode insn) {
		insns.add(insn);
		insn.addAttr(attr);
	}

	public void removeInsn(MethodNode mth, InsnNode insn) {
		insns.remove(insn);
		insn.remove(AType.CATCH_BLOCK);
		if (insns.isEmpty()) {
			removeWholeBlock(mth);
		}
	}

	public void removeBlock(MethodNode mth, BlockNode block) {
		for (InsnNode insn : block.getInstructions()) {
			insns.remove(insn);
			insn.remove(AType.CATCH_BLOCK);
		}
		if (insns.isEmpty()) {
			removeWholeBlock(mth);
		}
	}

	public Iterable<InsnNode> getInsns() {
		return insns;
	}

	public CatchAttr getCatchAttr() {
		return attr;
	}

	public boolean merge(MethodNode mth, TryCatchBlock tryBlock) {
		if (tryBlock == this) {
			return false;
		}

		for (InsnNode insn : tryBlock.getInsns()) {
			this.addInsn(insn);
		}
		this.handlers.addAll(tryBlock.handlers);
		for (ExceptionHandler eh : handlers) {
			eh.setTryBlock(this);
		}
		// clear
		tryBlock.handlers.clear();
		tryBlock.removeWholeBlock(mth);
		return true;
	}

	@Override
	public int hashCode() {
		return handlers.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		TryCatchBlock other = (TryCatchBlock) obj;
		return handlers.equals(other.handlers);
	}

	@Override
	public String toString() {
		return "Catch:{ " + Utils.listToString(handlers) + " }";
	}
}
