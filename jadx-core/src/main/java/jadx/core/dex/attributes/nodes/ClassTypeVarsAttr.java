package jadx.core.dex.attributes.nodes;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttribute;
import jadx.core.dex.instructions.args.ArgType;

public class ClassTypeVarsAttr implements IAttribute {
	public static final ClassTypeVarsAttr EMPTY = new ClassTypeVarsAttr(Collections.emptyList(), Collections.emptyMap());

	/**
	 * Type vars defined in current class
	 */
	private final List<ArgType> typeVars;

	/**
	 * Type vars mapping in current and super types:
	 * TypeRawObj -> (TypeVarInSuperType -> TypeVarFromThisClass)
	 */
	private final Map<String, Map<ArgType, ArgType>> superTypeMaps;

	public ClassTypeVarsAttr(List<ArgType> typeVars, Map<String, Map<ArgType, ArgType>> superTypeMaps) {
		this.typeVars = typeVars;
		this.superTypeMaps = superTypeMaps;
	}

	public List<ArgType> getTypeVars() {
		return typeVars;
	}

	public Map<String, Map<ArgType, ArgType>> getSuperTypeMaps() {
		return superTypeMaps;
	}

	@Override
	public AType<ClassTypeVarsAttr> getType() {
		return AType.CLASS_TYPE_VARS;
	}

	@Override
	public String toString() {
		return "ClassTypeVarsAttr{" + typeVars + ", super maps: " + superTypeMaps + '}';
	}
}
