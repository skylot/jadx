package jadx.core.clsp;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxRuntimeException;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classes hierarchy graph
 */
public class ClspGraph {
	private static final Logger LOG = LoggerFactory.getLogger(ClspGraph.class);

	private final Map<String, Set<String>> ancestorCache = new WeakHashMap<String, Set<String>>();
	private Map<String, NClass> nameMap;

	public void load() throws IOException, DecodeException {
		ClsSet set = new ClsSet();
		set.load();
		addClasspath(set);
	}

	public void addClasspath(ClsSet set) {
		if (nameMap == null) {
			nameMap = new HashMap<String, NClass>(set.getClassesCount());
			set.addToMap(nameMap);
		} else {
			throw new JadxRuntimeException("Classpath already loaded");
		}
	}

	public void addApp(List<ClassNode> classes) {
		if (nameMap == null) {
			throw new JadxRuntimeException("Classpath must be loaded first");
		}
		int size = classes.size();
		NClass[] nClasses = new NClass[size];
		for (int i = 0; i < size; i++) {
			ClassNode cls = classes.get(i);
			NClass nClass = new NClass(cls.getRawName(), -1);
			nClasses[i] = nClass;
			nameMap.put(cls.getRawName(), nClass);
		}
		for (int i = 0; i < size; i++) {
			nClasses[i].setParents(ClsSet.makeParentsArray(classes.get(i), nameMap));
		}
	}

	public boolean isImplements(String clsName, String implClsName) {
		Set<String> anc = getAncestors(clsName);
		return anc.contains(implClsName);
	}

	public String getCommonAncestor(String clsName, String implClsName) {
		if (isImplements(clsName, implClsName)) {
			return implClsName;
		}
		Set<String> anc = getAncestors(clsName);
		NClass cls = nameMap.get(implClsName);
		if (cls != null) {
			return searchCommonParent(anc, cls);
		} else {
			LOG.debug("Missing class: {}", implClsName);
			return null;
		}
	}

	private String searchCommonParent(Set<String> anc, NClass cls) {
		for (NClass p : cls.getParents()) {
			String name = p.getName();
			if (anc.contains(name)) {
				return name;
			} else {
				String r = searchCommonParent(anc, p);
				if (r != null) {
					return r;
				}
			}
		}
		return null;
	}

	private Set<String> getAncestors(String clsName) {
		Set<String> result = ancestorCache.get(clsName);
		if (result == null) {
			result = new HashSet<String>();
			ancestorCache.put(clsName, result);
			NClass cls = nameMap.get(clsName);
			if (cls != null) {
				addAncestorsNames(cls, result);
			} else {
				LOG.debug("Missing class: {}", clsName);
			}
		}
		return result;
	}

	private void addAncestorsNames(NClass cls, Set<String> result) {
		result.add(cls.getName());
		for (NClass p : cls.getParents()) {
			addAncestorsNames(p, result);
		}
	}
}
