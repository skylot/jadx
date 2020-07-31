package jadx.core.clsp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jadx.core.dex.instructions.args.ArgType;

/**
 * Class node in classpath graph
 */
public class ClspClass {

	private final ArgType clsType;
	private final int id;
	private ArgType[] parents;
	private Map<String, ClspMethod> methodsMap = Collections.emptyMap();
	private List<ArgType> typeParameters = Collections.emptyList();

	public ClspClass(ArgType clsType, int id) {
		this.clsType = clsType;
		this.id = id;
	}

	public String getName() {
		return clsType.getObject();
	}

	public ArgType getClsType() {
		return clsType;
	}

	public int getId() {
		return id;
	}

	public ArgType[] getParents() {
		return parents;
	}

	public void setParents(ArgType[] parents) {
		this.parents = parents;
	}

	public Map<String, ClspMethod> getMethodsMap() {
		return methodsMap;
	}

	public List<ClspMethod> getSortedMethodsList() {
		List<ClspMethod> list = new ArrayList<>(methodsMap.size());
		list.addAll(methodsMap.values());
		Collections.sort(list);
		return list;
	}

	public void setMethodsMap(Map<String, ClspMethod> methodsMap) {
		this.methodsMap = Objects.requireNonNull(methodsMap);
	}

	public void setMethods(List<ClspMethod> methods) {
		Map<String, ClspMethod> map = new HashMap<>(methods.size());
		for (ClspMethod mth : methods) {
			map.put(mth.getMethodInfo().getShortId(), mth);
		}
		setMethodsMap(map);
	}

	public List<ArgType> getTypeParameters() {
		return typeParameters;
	}

	public void setTypeParameters(List<ArgType> typeParameters) {
		this.typeParameters = typeParameters;
	}

	@Override
	public int hashCode() {
		return clsType.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ClspClass nClass = (ClspClass) o;
		return clsType.equals(nClass.clsType);
	}

	@Override
	public String toString() {
		return clsType.toString();
	}
}
