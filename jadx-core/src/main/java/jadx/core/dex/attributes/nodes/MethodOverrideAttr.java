package jadx.core.dex.attributes.nodes;

import java.util.List;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttribute;
import jadx.core.dex.nodes.IMethodDetails;

public class MethodOverrideAttr implements IAttribute {

	private final List<IMethodDetails> overrideList;

	public MethodOverrideAttr(List<IMethodDetails> overrideList) {
		this.overrideList = overrideList;
	}

	public List<IMethodDetails> getOverrideList() {
		return overrideList;
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
