package jadx.dex.instructions.args;

import java.util.ArrayList;
import java.util.List;

public class TypedVar {

	private ArgType type;
	private final List<InsnArg> useList = new ArrayList<InsnArg>(2);
	private String name;

	public TypedVar(ArgType initType) {
		this.type = initType;
	}

	public ArgType getType() {
		return type;
	}

	/**
	 * This method must be used very carefully
	 */
	public boolean forceSetType(ArgType type) {
		if (type != null && !type.equals(type)) {
			this.type = type;
			return true;
		} else {
			return false;
		}
	}

	public boolean merge(TypedVar typedVar) {
		return merge(typedVar.getType());
	}

	public boolean merge(ArgType mtype) {
		ArgType res = ArgType.merge(type, mtype);
		if (res != null && !type.equals(res)) {
			this.type = res;
			return true;
		} else {
			return false;
		}
	}

	public List<InsnArg> getUseList() {
		return useList;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public int hashCode() {
		return type.hashCode() * 31 + ((name == null) ? 0 : name.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		TypedVar other = (TypedVar) obj;
		if (name == null) {
			if (other.name != null) return false;
		} else if (!name.equals(other.name)) return false;
		if (type == null) {
			if (other.type != null) return false;
		} else if (!type.equals(other.type)) return false;
		return true;
	}

	@Override
	public String toString() {
		return (name != null ? "'" + name + "' " : "") + type.toString();
	}
}
