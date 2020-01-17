package jadx.core.dex.regions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jadx.core.codegen.CodeWriter;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IBranchRegion;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.utils.Utils;

public final class SwitchRegion extends AbstractRegion implements IBranchRegion {

	private final BlockNode header;

	private final List<CaseInfo> cases;
	private IContainer defCase;

	public SwitchRegion(IRegion parent, BlockNode header) {
		super(parent);
		this.header = header;
		this.cases = new ArrayList<>();
	}

	public static final class CaseInfo {
		private final List<Object> keys;
		private final IContainer container;

		public CaseInfo(List<Object> keys, IContainer container) {
			this.keys = keys;
			this.container = container;
		}

		public List<Object> getKeys() {
			return keys;
		}

		public IContainer getContainer() {
			return container;
		}
	}

	public BlockNode getHeader() {
		return header;
	}

	public void addCase(List<Object> keysList, IContainer c) {
		cases.add(new CaseInfo(keysList, c));
	}

	public void setDefaultCase(IContainer block) {
		defCase = block;
	}

	public IContainer getDefaultCase() {
		return defCase;
	}

	public List<CaseInfo> getCases() {
		return cases;
	}

	public List<IContainer> getCaseContainers() {
		return Utils.collectionMap(cases, caseInfo -> caseInfo.container);
	}

	@Override
	public List<IContainer> getSubBlocks() {
		List<IContainer> all = new ArrayList<>(cases.size() + 2);
		all.add(header);
		all.addAll(getCaseContainers());
		if (defCase != null) {
			all.add(defCase);
		}
		return Collections.unmodifiableList(all);
	}

	@Override
	public List<IContainer> getBranches() {
		List<IContainer> branches = new ArrayList<>(cases.size() + 1);
		branches.addAll(getCaseContainers());
		if (defCase != null) {
			branches.add(defCase);
		}
		return Collections.unmodifiableList(branches);
	}

	@Override
	public String baseString() {
		return header.baseString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Switch: ").append(cases.size());
		for (CaseInfo caseInfo : cases) {
			sb.append(CodeWriter.NL).append(" case ")
					.append(Utils.listToString(caseInfo.getKeys()))
					.append(" -> ").append(caseInfo.getContainer());
		}
		if (defCase != null) {
			sb.append(CodeWriter.NL).append(" default -> ").append(defCase);
		}
		return sb.toString();
	}
}
