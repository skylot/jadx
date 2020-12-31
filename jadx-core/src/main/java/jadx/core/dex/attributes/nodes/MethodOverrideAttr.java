package jadx.core.dex.attributes.nodes;

import java.util.List;
import java.util.SortedSet;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttribute;
import jadx.core.dex.nodes.IMethodDetails;
import jadx.core.dex.nodes.MethodNode;

public class MethodOverrideAttr implements IAttribute {

	/**
	 * All methods overridden by current method. Current method excluded, empty for base method.
	 */
	private List<IMethodDetails> overrideList;

	/**
	 * All method nodes from override hierarchy. Current method included.
	 */
	private SortedSet<MethodNode> relatedMthNodes;

	public MethodOverrideAttr(List<IMethodDetails> overrideList, SortedSet<MethodNode> relatedMthNodes) {
		this.overrideList = overrideList;
		this.relatedMthNodes = relatedMthNodes;
	}

	public boolean isAtBaseMth() {
		return overrideList.isEmpty();
	}

	public List<IMethodDetails> getOverrideList() {
		return overrideList;
	}

	public void setOverrideList(List<IMethodDetails> overrideList) {
		this.overrideList = overrideList;
	}

	public SortedSet<MethodNode> getRelatedMthNodes() {
		return relatedMthNodes;
	}

	public void setRelatedMthNodes(SortedSet<MethodNode> relatedMthNodes) {
		this.relatedMthNodes = relatedMthNodes;
	}

	@Override
	public AType<MethodOverrideAttr> getType() {
		return AType.METHOD_OVERRIDE;
	}

	@Override
	public String toString() {
		return "METHOD_OVERRIDE: " + overrideList;
	}
}
