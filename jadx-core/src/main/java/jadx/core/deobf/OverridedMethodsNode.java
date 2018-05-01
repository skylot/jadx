package jadx.core.deobf;

import java.util.Set;

import jadx.core.dex.info.MethodInfo;

class OverridedMethodsNode {

	private Set<MethodInfo> methods;

	public OverridedMethodsNode(Set<MethodInfo> methodsSet) {
		methods = methodsSet;
	}

	public boolean contains(MethodInfo mth) {
		return methods.contains(mth);
	}

	public void add(MethodInfo mth) {
		methods.add(mth);
	}

	public Set<MethodInfo> getMethods() {
		return methods;
	}
}
