package jadx.core.utils;

import java.util.Collections;
import java.util.Set;

public class CacheStorage {

	private Set<String> rootPkgs = Collections.emptySet();

	public Set<String> getRootPkgs() {
		return rootPkgs;
	}

	public void setRootPkgs(Set<String> rootPkgs) {
		this.rootPkgs = rootPkgs;
	}
}
