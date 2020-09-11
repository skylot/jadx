package jadx.core.dex.attributes.nodes;

import java.util.Collections;
import java.util.Set;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttribute;
import jadx.core.dex.instructions.args.ArgType;

import static jadx.core.utils.Utils.isEmpty;

/**
 * Set of known type variables at current method
 */
public class MethodTypeVarsAttr implements IAttribute {
	private static final MethodTypeVarsAttr EMPTY = new MethodTypeVarsAttr(Collections.emptySet());

	public static MethodTypeVarsAttr build(Set<ArgType> typeVars) {
		if (isEmpty(typeVars)) {
			return EMPTY;
		}
		return new MethodTypeVarsAttr(typeVars);
	}

	private final Set<ArgType> typeVars;

	private MethodTypeVarsAttr(Set<ArgType> typeVars) {
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
		if (this == EMPTY) {
			return "TYPE_VARS: EMPTY";
		}
		return "TYPE_VARS: " + typeVars;
	}
}
