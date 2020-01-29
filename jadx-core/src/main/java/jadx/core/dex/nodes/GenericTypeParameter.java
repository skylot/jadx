package jadx.core.dex.nodes;

import java.util.List;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.utils.Utils;

import static jadx.core.utils.Utils.notEmpty;

public class GenericTypeParameter {
	private final ArgType typeVariable;
	private final List<ArgType> extendsList;

	public GenericTypeParameter(ArgType typeVariable, List<ArgType> extendsList) {
		this.typeVariable = typeVariable;
		this.extendsList = extendsList;
	}

	public ArgType getTypeVariable() {
		return typeVariable;
	}

	public List<ArgType> getExtendsList() {
		return extendsList;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		GenericTypeParameter other = (GenericTypeParameter) o;
		return typeVariable.equals(other.typeVariable)
				&& extendsList.equals(other.extendsList);
	}

	@Override
	public int hashCode() {
		return 31 * typeVariable.hashCode() + extendsList.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(typeVariable);
		if (notEmpty(extendsList)) {
			sb.append(" extends ");
			sb.append(Utils.listToString(extendsList, " & "));
		}
		return sb.toString();
	}
}
