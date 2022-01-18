package jadx.core.dex.attributes.nodes;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import jadx.api.plugins.input.data.attributes.PinnedAttribute;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.IMethodDetails;
import jadx.core.dex.nodes.MethodNode;

public class MethodOverrideAttr extends PinnedAttribute {

	/**
	 * All methods overridden by current method. Current method excluded, empty for base method.
	 */
	private List<IMethodDetails> overrideList;

	/**
	 * All method nodes from override hierarchy. Current method included.
	 */
	private SortedSet<MethodNode> relatedMthNodes;

	private Set<IMethodDetails> baseMethods;

	public MethodOverrideAttr(List<IMethodDetails> overrideList, SortedSet<MethodNode> relatedMthNodes, Set<IMethodDetails> baseMethods) {
		this.overrideList = overrideList;
		this.relatedMthNodes = relatedMthNodes;
		this.baseMethods = baseMethods;
	}

	public List<IMethodDetails> getOverrideList() {
		return overrideList;
	}

	public SortedSet<MethodNode> getRelatedMthNodes() {
		return relatedMthNodes;
	}

	public Set<IMethodDetails> getBaseMethods() {
		return baseMethods;
	}

	public void setRelatedMthNodes(SortedSet<MethodNode> relatedMthNodes) {
		this.relatedMthNodes = relatedMthNodes;
	}

	@Override
	public AType<MethodOverrideAttr> getAttrType() {
		return AType.METHOD_OVERRIDE;
	}

	@Override
	public String toString() {
		return "METHOD_OVERRIDE: " + getBaseMethods();
	}
}
