package jadx.dex.regions;

import jadx.dex.instructions.args.RegisterArg;
import jadx.dex.nodes.IContainer;
import jadx.dex.nodes.IRegion;

import java.util.List;

public final class SynchronizedRegion extends AbstractRegion {

	private final RegisterArg arg;
	private final Region region;

	public SynchronizedRegion(IRegion parent, RegisterArg arg) {
		super(parent);
		this.arg = arg;
		this.region = new Region(this);
	}

	public RegisterArg getArg() {
		return arg;
	}

	public Region getRegion() {
		return region;
	}

	@Override
	public List<IContainer> getSubBlocks() {
		return region.getSubBlocks();
	}

	@Override
	public String toString() {
		return "Synchronized:" + region;
	}
}
