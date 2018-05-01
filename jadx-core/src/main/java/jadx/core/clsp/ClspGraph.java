package jadx.core.clsp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxRuntimeException;

/**
 * Classes hierarchy graph
 */
public class ClspGraph {
	private static final Logger LOG = LoggerFactory.getLogger(ClspGraph.class);

	private final Map<String, Set<String>> ancestorCache = Collections.synchronizedMap(new WeakHashMap<String, Set<String>>());
	private Map<String, NClass> nameMap;

	private final Set<String> missingClasses = new HashSet<>();

	public void load() throws IOException, DecodeException {
		ClsSet set = new ClsSet();
		set.load();
		addClasspath(set);
	}

	public void addClasspath(ClsSet set) {
		if (nameMap == null) {
			nameMap = new HashMap<>(set.getClassesCount());
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
		int k = 0;
		for (ClassNode cls : classes) {
			nClasses[k++] = addClass(cls);
		}
		for (int i = 0; i < size; i++) {
			nClasses[i].setParents(ClsSet.makeParentsArray(classes.get(i), nameMap));
		}
	}

	private NClass addClass(ClassNode cls) {
		String rawName = cls.getRawName();
		NClass nClass = new NClass(rawName, -1);
		nameMap.put(rawName, nClass);
		return nClass;
	}

	public boolean isImplements(String clsName, String implClsName) {
		Set<String> anc = getAncestors(clsName);
		return anc.contains(implClsName);
	}

	public String getCommonAncestor(String clsName, String implClsName) {
		if (clsName.equals(implClsName)) {
			return clsName;
		}
		NClass cls = nameMap.get(implClsName);
		if (cls == null) {
			missingClasses.add(clsName);
			return null;
		}
		if (isImplements(clsName, implClsName)) {
			return implClsName;
		}
		Set<String> anc = getAncestors(clsName);
		return searchCommonParent(anc, cls);
	}

	private String searchCommonParent(Set<String> anc, NClass cls) {
		for (NClass p : cls.getParents()) {
			String name = p.getName();
			if (anc.contains(name)) {
				return name;
			}
			String r = searchCommonParent(anc, p);
			if (r != null) {
				return r;
			}
		}
		return null;
	}

	private Set<String> getAncestors(String clsName) {
		Set<String> result = ancestorCache.get(clsName);
		if (result != null) {
			return result;
		}
		NClass cls = nameMap.get(clsName);
		if (cls == null) {
			missingClasses.add(clsName);
			return Collections.emptySet();
		}
		result = new HashSet<>();
		addAncestorsNames(cls, result);
		if (result.isEmpty()) {
			result = Collections.emptySet();
		}
		ancestorCache.put(clsName, result);
		return result;
	}

	private void addAncestorsNames(NClass cls, Set<String> result) {
		boolean isNew = result.add(cls.getName());
		if (isNew) {
			for (NClass p : cls.getParents()) {
				addAncestorsNames(p, result);
			}
		}
	}

	public void printMissingClasses() {
		int count = missingClasses.size();
		if (count == 0) {
			return;
		}
		LOG.warn("Found {} references to unknown classes", count);
		if (LOG.isDebugEnabled()) {
			List<String> clsNames = new ArrayList<>(missingClasses);
			Collections.sort(clsNames);
			for (String cls : clsNames) {
				LOG.debug("  {}", cls);
			}
		}
	}
}
