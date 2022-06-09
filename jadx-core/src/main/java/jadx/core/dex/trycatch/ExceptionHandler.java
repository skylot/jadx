package jadx.core.dex.trycatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import jadx.core.Consts;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.Utils;

public class ExceptionHandler {

	private final List<ClassInfo> catchTypes = new ArrayList<>(1);
	private final int handlerOffset;

	private BlockNode handlerBlock;
	private final List<BlockNode> blocks = new ArrayList<>();
	private IContainer handlerRegion;
	private InsnArg arg;

	private TryCatchBlockAttr tryBlock;
	private boolean isFinally;

	private boolean removed = false;

	public static ExceptionHandler build(MethodNode mth, int addr, @Nullable ClassInfo type) {
		ExceptionHandler eh = new ExceptionHandler(addr);
		eh.addCatchType(mth, type);
		return eh;
	}

	private ExceptionHandler(int addr) {
		this.handlerOffset = addr;
	}

	/**
	 * Add exception type to catch block
	 *
	 * @param type - null for 'all' or 'Throwable' handler
	 */
	public boolean addCatchType(MethodNode mth, @Nullable ClassInfo type) {
		if (type != null) {
			if (catchTypes.contains(type)) {
				return false;
			}
			return catchTypes.add(type);
		}
		if (!this.catchTypes.isEmpty()) {
			mth.addDebugComment("Throwable added to exception handler: '" + catchTypeStr() + "', keep only Throwable");
			catchTypes.clear();
			return true;
		}
		return false;
	}

	public void addCatchTypes(MethodNode mth, Collection<ClassInfo> types) {
		for (ClassInfo type : types) {
			addCatchType(mth, type);
		}
	}

	public List<ClassInfo> getCatchTypes() {
		return catchTypes;
	}

	public ArgType getArgType() {
		if (isCatchAll()) {
			return ArgType.THROWABLE;
		}
		List<ClassInfo> types = getCatchTypes();
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

	public int getHandlerOffset() {
		return handlerOffset;
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

	public void setTryBlock(TryCatchBlockAttr tryBlock) {
		this.tryBlock = tryBlock;
	}

	public TryCatchBlockAttr getTryBlock() {
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
		this.blocks.forEach(b -> b.add(AFlag.REMOVE));
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
		return handlerOffset == that.handlerOffset
				&& catchTypes.equals(that.catchTypes)
				&& Objects.equals(tryBlock, that.tryBlock);
	}

	@Override
	public int hashCode() {
		return Objects.hash(catchTypes, handlerOffset /* , tryBlock */);
	}

	public String catchTypeStr() {
		return catchTypes.isEmpty() ? "all" : Utils.listToString(catchTypes, " | ", ClassInfo::getShortName);
	}

	@Override
	public String toString() {
		return catchTypeStr() + " -> " + InsnUtils.formatOffset(handlerOffset);
	}
}
