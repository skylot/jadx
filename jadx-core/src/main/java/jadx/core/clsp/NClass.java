package jadx.core.clsp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jadx.core.dex.nodes.GenericInfo;

/**
 * Class node in classpath graph
 */
public class NClass {

	private final String name;
	private final int id;
	private NClass[] parents;
	private Map<String, NMethod> methodsMap = Collections.emptyMap();
	private List<GenericInfo> generics = Collections.emptyList();

	public NClass(String name, int id) {
		this.name = name;
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public int getId() {
		return id;
	}

	public NClass[] getParents() {
		return parents;
	}

	public void setParents(NClass[] parents) {
		this.parents = parents;
	}

	public Map<String, NMethod> getMethodsMap() {
		return methodsMap;
	}

	public List<NMethod> getMethodsList() {
		List<NMethod> list = new ArrayList<>(methodsMap.size());
		list.addAll(methodsMap.values());
		Collections.sort(list);
		return list;
	}

	public void setMethodsMap(Map<String, NMethod> methodsMap) {
		this.methodsMap = Objects.requireNonNull(methodsMap);
	}

	public void setMethods(List<NMethod> methods) {
		Map<String, NMethod> map = new HashMap<>(methods.size());
		for (NMethod mth : methods) {
			map.put(mth.getShortId(), mth);
		}
		setMethodsMap(map);
	}

	public List<GenericInfo> getGenerics() {
		return generics;
	}

	public void setGenerics(List<GenericInfo> generics) {
		this.generics = generics;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		NClass nClass = (NClass) o;
		return name.equals(nClass.name);
	}

	@Override
	public String toString() {
		return name;
	}
}
