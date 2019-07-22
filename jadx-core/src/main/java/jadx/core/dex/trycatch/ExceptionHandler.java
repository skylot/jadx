package jadx.core.dex.trycatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.jetbrains.annotations.Nullable;

import jadx.core.Consts;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IContainer;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class ExceptionHandler {

	private final Set<ClassInfo> catchTypes = new TreeSet<>();
	private final int handleOffset;

	private BlockNode handlerBlock;
	private final List<BlockNode> blocks = new ArrayList<>();
	private IContainer handlerRegion;
	private InsnArg arg;

	private TryCatchBlock tryBlock;
	private boolean isFinally;

	private boolean removed = false;

	public ExceptionHandler(int addr, @Nullable ClassInfo type) {
		this.handleOffset = addr;
		addCatchType(type);
	}

	/**
	 * Add exception type to catch block
	 *
	 * @param type - null for 'all' or 'Throwable' handler
	 */
	public void addCatchType(@Nullable ClassInfo type) {
		if (type != null) {
			this.catchTypes.add(type);
		} else {
			if (!this.catchTypes.isEmpty()) {
				throw new JadxRuntimeException("Null type added to not empty exception handler: " + this);
			}
		}
	}

	public void addCatchTypes(Collection<ClassInfo> types) {
		for (ClassInfo type : types) {
			addCatchType(type);
		}
	}

	public Set<ClassInfo> getCatchTypes() {
		return catchTypes;
	}

	public ArgType getArgType() {
		if (isCatchAll()) {
			return ArgType.THROWABLE;
		}
		Set<ClassInfo> types = getCatchTypes();
		if (types.size() == 1) {
			return types.iterator().next().getType();
		} else {
			return ArgType.THROWABLE;
		}
	}

	public boolean isCatchAll() {
		if (catchTypes.isEmpty()) {
			return true;
		}
		for (ClassInfo classInfo : catchTypes) {
			if (classInfo.getFullName().equals(Consts.CLASS_THROWABLE)) {
				return true;
			}
		}
		return false;
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

	public boolean isRemoved() {
		return removed;
	}

	public void markForRemove() {
		this.removed = true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ExceptionHandler that = (ExceptionHandler) o;
		return handleOffset == that.handleOffset
				&& catchTypes.equals(that.catchTypes)
				&& Objects.equals(tryBlock, that.tryBlock);
	}

	@Override
	public int hashCode() {
		return Objects.hash(catchTypes, handleOffset /* , tryBlock */);
	}

	public String catchTypeStr() {
		return catchTypes.isEmpty() ? "all" : Utils.listToString(catchTypes, " | ", ClassInfo::getShortName);
	}

	@Override
	public String toString() {
		return catchTypeStr() + " -> " + InsnUtils.formatOffset(handleOffset);
	}
}
