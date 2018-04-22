package jadx.core.dex.trycatch;

import java.util.ArrayList;
import java.util.List;

import jadx.core.Consts;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IContainer;
import jadx.core.utils.InsnUtils;

public class ExceptionHandler {

	private final ClassInfo catchType;
	private final int handleOffset;

	private BlockNode handlerBlock;
	private final List<BlockNode> blocks = new ArrayList<>();
	private IContainer handlerRegion;
	private InsnArg arg;

	private TryCatchBlock tryBlock;
	private boolean isFinally;

	public ExceptionHandler(int addr, ClassInfo type) {
		this.handleOffset = addr;
		this.catchType = type;
	}

	public ClassInfo getCatchType() {
		return catchType;
	}

	public boolean isCatchAll() {
		return catchType == null || catchType.getFullName().equals(Consts.CLASS_THROWABLE);
	}

	public int getHandleOffset() {
		return handleOffset;
	}

	public BlockNode getHandlerBlock() {
		return handlerBlock;
	}

	public void setHandlerBlock(BlockNode handlerBlock) {
		this.handlerBlock = handlerBlock;
	}

	public List<BlockNode> getBlocks() {
		return blocks;
	}

	public void addBlock(BlockNode node) {
		blocks.add(node);
	}

	public IContainer getHandlerRegion() {
		return handlerRegion;
	}

	public void setHandlerRegion(IContainer handlerRegion) {
		this.handlerRegion = handlerRegion;
	}

	public InsnArg getArg() {
		return arg;
	}

	public void setArg(InsnArg arg) {
		this.arg = arg;
	}

	public void setTryBlock(TryCatchBlock tryBlock) {
		this.tryBlock = tryBlock;
	}

	public TryCatchBlock getTryBlock() {
		return tryBlock;
	}

	public boolean isFinally() {
		return isFinally;
	}

	public void setFinally(boolean isFinally) {
		this.isFinally = isFinally;
	}

	@Override
	public int hashCode() {
		return (catchType == null ? 0 : 31 * catchType.hashCode()) + handleOffset;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ExceptionHandler other = (ExceptionHandler) obj;
		if (catchType == null) {
			if (other.catchType != null) {
				return false;
			}
		} else if (!catchType.equals(other.catchType)) {
			return false;
		}
		return handleOffset == other.handleOffset;
	}

	@Override
	public String toString() {
		return (catchType == null ? "all"
				: catchType.getShortName()) + " -> " + InsnUtils.formatOffset(handleOffset);
	}

}
