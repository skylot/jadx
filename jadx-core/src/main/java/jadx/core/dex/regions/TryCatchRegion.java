package jadx.core.dex.regions;

import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.trycatch.TryCatchBlock;
import jadx.core.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TryCatchRegion extends AbstractRegion {

	private final IContainer tryRegion;
	private List<IContainer> catchRegions = Collections.emptyList();
	private TryCatchBlock tryCatchBlock;

	public TryCatchRegion(IRegion parent, IContainer tryRegion) {
		super(parent);
		this.tryRegion = tryRegion;
	}

	public IContainer getTryRegion() {
		return tryRegion;
	}

	public List<IContainer> getCatchRegions() {
		return catchRegions;
	}

	public TryCatchBlock geTryCatchBlock() {
		return tryCatchBlock;
	}

	public void setTryCatchBlock(TryCatchBlock tryCatchBlock) {
		this.tryCatchBlock = tryCatchBlock;
		this.catchRegions = new ArrayList<IContainer>(tryCatchBlock.getHandlersCount());
		for (ExceptionHandler handler : tryCatchBlock.getHandlers()) {
			catchRegions.add(handler.getHandlerRegion());
		}
	}

	@Override
	public List<IContainer> getSubBlocks() {
		List<IContainer> all = new ArrayList<IContainer>(1 + catchRegions.size());
		all.add(tryRegion);
		all.addAll(catchRegions);
		return Collections.unmodifiableList(all);
	}

	@Override
	public String baseString() {
		return tryRegion.baseString();
	}

	@Override
	public String toString() {
		return "Try: " + tryRegion
				+ " catches: " + Utils.listToString(catchRegions);
	}
}
