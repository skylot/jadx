package jadx.dex.trycatch;

import jadx.dex.attributes.AttributeType;
import jadx.dex.info.ClassInfo;
import jadx.dex.nodes.BlockNode;
import jadx.dex.nodes.IContainer;
import jadx.dex.nodes.InsnNode;
import jadx.dex.nodes.MethodNode;
import jadx.utils.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class TryCatchBlock {

	private final List<ExceptionHandler> handlers;
	private IContainer finalBlock;

	// references for fast remove/modify
	private final List<InsnNode> insns;
	private final CatchAttr attr;

	public TryCatchBlock() {
		handlers = new ArrayList<ExceptionHandler>(2);
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
		return handler;
	}

	public void removeHandler(MethodNode mth, ExceptionHandler handler) {
		for (int i = 0; i < handlers.size(); i++) {
			if (handlers.get(i) == handler) {
				handlers.remove(i);
				break;
			}
		}
		if (handlers.isEmpty()) {
			removeWholeBlock(mth);
		}
	}

	private void removeWholeBlock(MethodNode mth) {
		// self destruction
		for (InsnNode insn : insns)
			insn.getAttributes().remove(AttributeType.CATCH_BLOCK);

		insns.clear();
		for (BlockNode block : mth.getBasicBlocks()) {
			block.getAttributes().remove(AttributeType.CATCH_BLOCK);
		}
	}

	public void addInsn(InsnNode insn) {
		insns.add(insn);
		insn.getAttributes().add(attr);
	}

	public void removeInsn(InsnNode insn) {
		insns.remove(insn);
		insn.getAttributes().remove(attr.getType());
	}

	public Iterable<InsnNode> getInsns() {
		return insns;
	}

	public int getInsnsCount() {
		return insns.size();
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

	public void merge(MethodNode mth, TryCatchBlock tryBlock) {
		for (InsnNode insn : tryBlock.getInsns())
			this.addInsn(insn);

		this.handlers.addAll(tryBlock.getHandlers());

		// clear
		tryBlock.handlers.clear();
		tryBlock.removeWholeBlock(mth);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((handlers == null) ? 0 : handlers.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		TryCatchBlock other = (TryCatchBlock) obj;
		if (!handlers.equals(other.handlers)) return false;
		return true;
	}

	@Override
	public String toString() {
		return "Catch:{ " + Utils.listToString(handlers) + " }";
	}

}
