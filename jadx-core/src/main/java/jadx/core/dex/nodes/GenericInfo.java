package jadx.core.dex.nodes;

import java.util.List;

import jadx.core.dex.instructions.args.ArgType;

public class GenericInfo {
	private final ArgType genericType;
	private final List<ArgType> extendsList;

	public GenericInfo(ArgType genericType, List<ArgType> extendsList) {
		this.genericType = genericType;
		this.extendsList = extendsList;
	}

	public ArgType getGenericType() {
		return genericType;
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
		GenericInfo other = (GenericInfo) o;
		return genericType.equals(other.genericType)
				&& extendsList.equals(other.extendsList);
	}

	@Override
	public int hashCode() {
		return 31 * genericType.hashCode() + extendsList.hashCode();
	}

	@Override
	public String toString() {
		return "GenericInfo{" + genericType + " extends: " + extendsList + '}';
	}
}
