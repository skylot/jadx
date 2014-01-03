package jadx.core.dex.instructions.args;

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
	public void forceSetType(ArgType newType) {
		type = newType;
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

	public void mergeName(TypedVar arg) {
		String name = arg.getName();
		if (name != null) {
			setName(name);
		} else if (getName() != null) {
			arg.setName(getName());
		}
	}

	public boolean isImmutable() {
		return false;
	}

	@Override
	public int hashCode() {
		return type.hashCode() * 31 + (name == null ? 0 : name.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TypedVar)) {
			return false;
		}
		TypedVar other = (TypedVar) obj;
		if (!type.equals(other.type)) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (name != null) {
			sb.append('\'').append(name).append("' ");
		}
		sb.append(type);
		return sb.toString();
	}
}
