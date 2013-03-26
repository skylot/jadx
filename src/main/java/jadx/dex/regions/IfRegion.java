package jadx.dex.regions;

import jadx.dex.instructions.IfNode;
import jadx.dex.nodes.BlockNode;
import jadx.dex.nodes.IContainer;
import jadx.dex.nodes.IRegion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class IfRegion extends AbstractRegion {

	protected BlockNode header;
	protected IContainer thenRegion;
	protected IContainer elseRegion;

	public IfRegion(IRegion parent, BlockNode header) {
		super(parent);
		assert header.getInstructions().size() == 1;
		this.header = header;
	}

	public IfNode getIfInsn() {
		return (IfNode) header.getInstructions().get(0);
	}

	public BlockNode getHeader() {
		return header;
	}

	public IContainer getThenRegion() {
		return thenRegion;
	}

	public void setThenRegion(IContainer thenRegion) {
		this.thenRegion = thenRegion;
	}

	public IContainer getElseRegion() {
		return elseRegion;
	}

	public void setElseRegion(IContainer elseRegion) {
		this.elseRegion = elseRegion;
	}

	@Override
	public List<IContainer> getSubBlocks() {
		ArrayList<IContainer> all = new ArrayList<IContainer>(3);
		all.add(header);
		if (thenRegion != null)
			all.add(thenRegion);
		if (elseRegion != null)
			all.add(elseRegion);
		return Collections.unmodifiableList(all);
	}

	@Override
	public String toString() {
		return "IF(" + header + ") then " + thenRegion + " else " + elseRegion;
	}
}
