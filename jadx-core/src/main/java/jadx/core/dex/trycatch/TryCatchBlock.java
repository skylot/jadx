package jadx.core.dex.trycatch;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.InsnContainer;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.InstructionRemover;
import jadx.core.utils.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class TryCatchBlock {

	private final List<ExceptionHandler> handlers;
	private IContainer finalBlock;

	// references for fast remove/modify
	private final List<InsnNode> insns;
	private final CatchAttr attr;

	public TryCatchBlock() {
		handlers = new LinkedList<ExceptionHandler>();
		insns = new ArrayList<InsnNode>();
		attr = new CatchAttr(this);
	}

	public Collection<ExceptionHandler> getHandlers() {
		return Collections.unmodifiableCollection(handlers);
	}

	public ExceptionHandler addHandler(MethodNode mth, int addr, ClassInfo type) {
		ExceptionHandler handler = new ExceptionHandler(addr, type);
		handler = mth.addExceptionHandler(handler);
		handlers.add(handler);
		handler.setTryBlock(this);
		return handler;
	}

	public void removeHandler(MethodNode mth, ExceptionHandler handler) {
		for (Iterator<ExceptionHandler> it = handlers.iterator(); it.hasNext(); ) {
			ExceptionHandler h = it.next();
			if (h == handler) {
				it.remove();
				break;
			}
		}
		if (handlers.isEmpty()) {
			removeWholeBlock(mth);
		}
	}

	private void removeWholeBlock(MethodNode mth) {
		if (finalBlock != null) {
			// search catch attr
			for (BlockNode block : mth.getBasicBlocks()) {
				CatchAttr cb = block.get(AType.CATCH_BLOCK);
				if (cb == attr) {
					for (ExceptionHandler eh : mth.getExceptionHandlers()) {
						if (eh.getBlocks().contains(block)) {
							TryCatchBlock tb = eh.getTryBlock();
							tb.setFinalBlockFromInsns(mth, ((IBlock) finalBlock).getInstructions());
						}
					}
				}
			}
		} else {
			// self destruction
			for (InsnNode insn : insns) {
				insn.removeAttr(attr);
			}
			insns.clear();
			for (BlockNode block : mth.getBasicBlocks()) {
				block.removeAttr(attr);
			}
		}
	}

	public void addInsn(InsnNode insn) {
		insns.add(insn);
		insn.addAttr(attr);
	}

	public void removeInsn(InsnNode insn) {
		insns.remove(insn);
		insn.remove(AType.CATCH_BLOCK);
	}

	public Iterable<InsnNode> getInsns() {
		return insns;
	}

	public CatchAttr getCatchAttr() {
		return attr;
	}

	public IContainer getFinalBlock() {
		return finalBlock;
	}

	public void setFinalBlock(IContainer finalBlock) {
		this.finalBlock = finalBlock;
	}

	public void setFinalBlockFromInsns(MethodNode mth, List<InsnNode> insns) {
		List<InsnNode> finalBlockInsns = new ArrayList<InsnNode>(insns);
		setFinalBlock(new InsnContainer(finalBlockInsns));

		InstructionRemover.unbindInsnList(mth, finalBlockInsns);

		// remove these instructions from other handlers
		for (ExceptionHandler h : getHandlers()) {
			for (BlockNode ehb : h.getBlocks()) {
				ehb.getInstructions().removeAll(finalBlockInsns);
			}
		}
		// remove from blocks with this catch
		for (BlockNode b : mth.getBasicBlocks()) {
			CatchAttr ca = b.get(AType.CATCH_BLOCK);
			if (attr == ca) {
				b.getInstructions().removeAll(finalBlockInsns);
			}
		}
	}

	public void merge(MethodNode mth, TryCatchBlock tryBlock) {
		for (InsnNode insn : tryBlock.getInsns()) {
			this.addInsn(insn);
		}
		this.handlers.addAll(tryBlock.getHandlers());
		for (ExceptionHandler eh : handlers) {
			eh.setTryBlock(this);
		}
		// clear
		tryBlock.handlers.clear();
		tryBlock.removeWholeBlock(mth);
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
