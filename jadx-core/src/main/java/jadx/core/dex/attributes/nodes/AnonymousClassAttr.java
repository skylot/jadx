package jadx.core.dex.attributes.nodes;

import jadx.api.plugins.input.data.attributes.PinnedAttribute;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;

public class AnonymousClassAttr extends PinnedAttribute {

	private final ClassNode outerCls;
	private final ArgType baseType;

	public AnonymousClassAttr(ClassNode outerCls, ArgType baseType) {
		this.outerCls = outerCls;
		this.baseType = baseType;
	}

	public ClassNode getOuterCls() {
		return outerCls;
	}

	public ArgType getBaseType() {
		return baseType;
	}

	@Override
	public AType<AnonymousClassAttr> getAttrType() {
		return AType.ANONYMOUS_CLASS;
	}

	@Override
	public String toString() {
		return "AnonymousClass{" + outerCls + ", base: " + baseType + '}';
	}
}
