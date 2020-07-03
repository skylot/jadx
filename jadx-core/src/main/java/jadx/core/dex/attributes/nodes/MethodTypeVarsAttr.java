package jadx.core.dex.attributes.nodes;

import java.util.Set;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttribute;
import jadx.core.dex.instructions.args.ArgType;

/**
 * Set of known type variables at current method
 */
public class MethodTypeVarsAttr implements IAttribute {

	private final Set<ArgType> typeVars;

	public MethodTypeVarsAttr(Set<ArgType> typeVars) {
		this.typeVars = typeVars;
	}

	public Set<ArgType> getTypeVars() {
		return typeVars;
	}

	@Override
	public AType<MethodTypeVarsAttr> getType() {
		return AType.METHOD_TYPE_VARS;
	}

	@Override
	public String toString() {
		return "TYPE_VARS: " + typeVars;
	}
}
