package jadx.core.dex.regions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jadx.api.ICodeWriter;
import jadx.core.codegen.RegionGen;
import jadx.core.dex.nodes.IBranchRegion;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.trycatch.TryCatchBlockAttr;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.CodegenException;

public final class TryCatchRegion extends AbstractRegion implements IBranchRegion {

	private final IContainer tryRegion;
	private Map<ExceptionHandler, IContainer> catchRegions = Collections.emptyMap();
	private IContainer finallyRegion;
	private TryCatchBlockAttr tryCatchBlock;

	public TryCatchRegion(IRegion parent, IContainer tryRegion) {
		super(parent);
		this.tryRegion = tryRegion;
	}

	public void setTryCatchBlock(TryCatchBlockAttr tryCatchBlock) {
		this.tryCatchBlock = tryCatchBlock;
		int count = tryCatchBlock.getHandlersCount();
		this.catchRegions = new LinkedHashMap<>(count);
		for (ExceptionHandler handler : tryCatchBlock.getHandlers()) {
			IContainer handlerRegion = handler.getHandlerRegion();
			if (handlerRegion != null) {
				if (handler.isFinally()) {
					finallyRegion = handlerRegion;
				} else {
					catchRegions.put(handler, handlerRegion);
				}
			}
		}
	}

	public IContainer getTryRegion() {
		return tryRegion;
	}

	public Map<ExceptionHandler, IContainer> getCatchRegions() {
		return catchRegions;
	}

	public TryCatchBlockAttr getTryCatchBlock() {
		return tryCatchBlock;
	}

	public IContainer getFinallyRegion() {
		return finallyRegion;
	}

	public void setFinallyRegion(IContainer finallyRegion) {
		this.finallyRegion = finallyRegion;
	}

	@Override
	public List<IContainer> getSubBlocks() {
		List<IContainer> all = new ArrayList<>(2 + catchRegions.size());
		all.add(tryRegion);
		all.addAll(catchRegions.values());
		if (finallyRegion != null) {
			all.add(finallyRegion);
		}
		return Collections.unmodifiableList(all);
	}

	@Override
	public List<IContainer> getBranches() {
		return getSubBlocks();
	}

	@Override
	public void generate(RegionGen regionGen, ICodeWriter code) throws CodegenException {
		regionGen.makeTryCatch(this, code);
	}

	@Override
	public String baseString() {
		return tryRegion.baseString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Try: ").append(tryRegion);
		if (!catchRegions.isEmpty()) {
			sb.append(" catches: ").append(Utils.listToString(catchRegions.values()));
		}
		if (finallyRegion != null) {
			sb.append(" finally: ").append(finallyRegion);
		}
		return sb.toString();
	}
}
