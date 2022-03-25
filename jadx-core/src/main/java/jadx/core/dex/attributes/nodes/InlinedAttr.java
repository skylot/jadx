package jadx.core.dex.attributes.nodes;

import jadx.api.plugins.input.data.attributes.IJadxAttrType;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.ClassNode;

public class InlinedAttr implements IJadxAttribute {

	private final ClassNode inlineCls;

	public InlinedAttr(ClassNode inlineCls) {
		this.inlineCls = inlineCls;
	}

	public ClassNode getInlineCls() {
		return inlineCls;
	}

	@Override
	public IJadxAttrType<InlinedAttr> getAttrType() {
		return AType.INLINED;
	}

	@Override
	public String toString() {
		return "INLINED: " + inlineCls;
	}
}
