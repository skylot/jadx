package jadx.core.dex.regions;

import java.util.ArrayList;
import java.util.List;

import jadx.api.ICodeWriter;
import jadx.core.codegen.RegionGen;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.exceptions.CodegenException;

public final class SynchronizedRegion extends AbstractRegion {

	private final InsnNode enterInsn;
	private final List<InsnNode> exitInsns = new ArrayList<>();
	private final Region region;

	public SynchronizedRegion(IRegion parent, InsnNode insn) {
		super(parent);
		this.enterInsn = insn;
		this.region = new Region(this);
	}

	public InsnNode getEnterInsn() {
		return enterInsn;
	}

	public List<InsnNode> getExitInsns() {
		return exitInsns;
	}

	public Region getRegion() {
		return region;
	}

	@Override
	public List<IContainer> getSubBlocks() {
		return region.getSubBlocks();
	}

	@Override
	public void generate(RegionGen regionGen, ICodeWriter code) throws CodegenException {
		regionGen.makeSynchronizedRegion(this, code);
	}

	@Override
	public String baseString() {
		return Integer.toHexString(enterInsn.getOffset());
	}

	@Override
	public String toString() {
		return "Synchronized:" + region;
	}
}
