package jadx.dex.regions;

import jadx.dex.nodes.BlockNode;
import jadx.dex.nodes.IContainer;
import jadx.dex.nodes.IRegion;

import java.util.ArrayList;
import java.util.List;

public final class SwitchRegion extends AbstractRegion {

	private final BlockNode header;

	private final List<List<Integer>> keys;
	private final List<IContainer> cases;
	private IContainer defCase;

	public SwitchRegion(IRegion parent, BlockNode header) {
		super(parent);
		this.header = header;
		this.keys = new ArrayList<List<Integer>>();
		this.cases = new ArrayList<IContainer>();
	}

	public BlockNode getHeader() {
		return header;
	}

	public void addCase(List<Integer> keysList, IContainer c) {
		keys.add(keysList);
		cases.add(c);
	}

	public void setDefaultCase(IContainer block) {
		defCase = block;
	}

	public IContainer getDefaultCase() {
		return defCase;
	}

	public List<List<Integer>> getKeys() {
		return keys;
	}

	public List<IContainer> getCases() {
		return cases;
	}

	@Override
	public List<IContainer> getSubBlocks() {
		List<IContainer> all = new ArrayList<IContainer>(cases.size() + 2);
		all.add(header);
		all.addAll(cases);
		if (defCase != null)
			all.add(defCase);
		return all;
	}

	@Override
	public String toString() {
		return "Switch: " + cases.size() + ", default: " + defCase;
	}
}
