package jadx.core.deobf;

import jadx.core.dex.info.MethodInfo;

import java.util.Set;

/* package */ class OverridedMethodsNode {

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
