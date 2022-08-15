package jadx.core.dex.trycatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jadx.api.plugins.input.data.attributes.IJadxAttrType;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.Utils;

public class TryCatchBlockAttr implements IJadxAttribute {

	private final int id;
	private final List<ExceptionHandler> handlers;
	private List<BlockNode> blocks;

	private TryCatchBlockAttr outerTryBlock;
	private List<TryCatchBlockAttr> innerTryBlocks = Collections.emptyList();
	private boolean merged = false;

	private BlockNode topSplitter;

	public TryCatchBlockAttr(int id, List<ExceptionHandler> handlers, List<BlockNode> blocks) {
		this.id = id;
		this.handlers = handlers;
		this.blocks = blocks;

		handlers.forEach(h -> h.setTryBlock(this));
	}

	public boolean isAllHandler() {
		return handlers.size() == 1 && handlers.get(0).isCatchAll();
	}

	public boolean isThrowOnly() {
		boolean throwFound = false;
		for (BlockNode block : blocks) {
			List<InsnNode> insns = block.getInstructions();
			if (insns.size() != 1) {
				return false;
			}
			InsnNode insn = insns.get(0);
			switch (insn.getType()) {
				case MOVE_EXCEPTION:
				case MONITOR_EXIT:
					// allowed instructions
					break;

				case THROW:
					throwFound = true;
					break;

				default:
					return false;
			}
		}
		return throwFound;
	}

	public int getId() {
		return id;
	}

	public List<ExceptionHandler> getHandlers() {
		return handlers;
	}

	public int getHandlersCount() {
		return handlers.size();
	}

	public List<BlockNode> getBlocks() {
		return blocks;
	}

	public void setBlocks(List<BlockNode> blocks) {
		this.blocks = blocks;
	}

	public void clear() {
		blocks.clear();
		handlers.forEach(ExceptionHandler::markForRemove);
		handlers.clear();
	}

	public void removeBlock(BlockNode block) {
		blocks.remove(block);
	}

	public void removeHandler(ExceptionHandler handler) {
		handlers.remove(handler);
		handler.markForRemove();
	}

	public List<TryCatchBlockAttr> getInnerTryBlocks() {
		return innerTryBlocks;
	}

	public void addInnerTryBlock(TryCatchBlockAttr inner) {
		if (this.innerTryBlocks.isEmpty()) {
			this.innerTryBlocks = new ArrayList<>();
		}
		this.innerTryBlocks.add(inner);
	}

	public TryCatchBlockAttr getOuterTryBlock() {
		return outerTryBlock;
	}

	public void setOuterTryBlock(TryCatchBlockAttr outerTryBlock) {
		this.outerTryBlock = outerTryBlock;
	}

	public BlockNode getTopSplitter() {
		return topSplitter;
	}

	public void setTopSplitter(BlockNode topSplitter) {
		this.topSplitter = topSplitter;
	}

	public boolean isMerged() {
		return merged;
	}

	public void setMerged(boolean merged) {
		this.merged = merged;
	}

	public int id() {
		return id;
	}

	@Override
	public IJadxAttrType<? extends IJadxAttribute> getAttrType() {
		return AType.TRY_BLOCK;
	}

	@Override
	public int hashCode() {
		return handlers.hashCode() + 31 * blocks.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		TryCatchBlockAttr other = (TryCatchBlockAttr) obj;
		return id == other.id
				&& handlers.equals(other.handlers)
				&& blocks.equals(other.blocks);
	}

	@Override
	public String toString() {
		if (merged) {
			return "Merged into " + outerTryBlock;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("TryCatch #").append(id).append(" {").append(Utils.listToString(handlers));
		sb.append(", blocks: (").append(Utils.listToString(blocks)).append(')');
		if (topSplitter != null) {
			sb.append(", top: ").append(topSplitter);
		}
		if (outerTryBlock != null) {
			sb.append(", outer: #").append(outerTryBlock.id);
		}
		if (!innerTryBlocks.isEmpty()) {
			sb.append(", inners: ").append(Utils.listToString(innerTryBlocks, inner -> "#" + inner.id));
		}
		sb.append(" }");
		return sb.toString();
	}
}
