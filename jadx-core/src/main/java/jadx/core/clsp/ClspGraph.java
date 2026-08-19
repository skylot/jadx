package jadx.core.clsp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.IMethodDetails;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxRuntimeException;

/**
 * Classes hierarchy graph with methods additional info
 *
 * nameMap is constructed through loadClsSetFile, addClasspath, or addApp.
 *
 * initCache must be called after the nameMap has been propagated to fill the caches before using
 * other class features.
 *
 * isImplements, getCommonAncestor, getImplementations and others can then be used to query the
 * class hierarchy.
 *
 * printMissingClasses is used to display any classes encountered when enumerating parents that
 * were missing from the nameMap.
 */
public class ClspGraph {
	private static final Logger LOG = LoggerFactory.getLogger(ClspGraph.class);

	private final RootNode root;
	/** Maps class names to class details */
	private Map<String, ClspClass> nameMap;
	/** Maps class names to all super classes and implemented interfaces */
	private Map<String, Set<String>> superTypesCache;
	/** Maps class names to all sub classes and implementations */
	private Map<String, List<String>> implementsCache;
	/**
	 * Maps class names to immediate super classes - this is equivalent to the .parents attribute of the
	 * ClspClasses in the nameMap
	 */
	private Map<String, Set<String>> immediateSuperTypesCache;
	/** Maps class names to immeditae sub classes and implementations */
	private Map<String, List<String>> immediateImplementsCache;

	/** Classes encountered when building the caches that have not had details provided */
	private final Set<String> missingClasses = new HashSet<>();

	public ClspGraph(RootNode rootNode) {
		this.root = rootNode;
	}

	public void loadClsSetFile() throws IOException, DecodeException {
		ClsSet set = new ClsSet(root);
		set.loadFromClstFile();
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

	/**
	 * Add a list of classes to the class map
	 *
	 * @param classes the classes to add
	 */
	public void addApp(List<ClassNode> classes) {
		if (nameMap == null) {
			nameMap = new HashMap<>(classes.size());
		}
		for (ClassNode cls : classes) {
			addClass(cls);
		}
	}

	/**
	 * Construct the cached mappings from the added classes
	 */
	public void initCache() {
		fillImmediateSuperTypesCache();
		fillImmediateImplementsCache();
		fillSuperTypesCache();
		fillImplementsCache();
	}

	/**
	 * Check if the class name has been added to the graph
	 *
	 * @param fullName the raw name of the class to check
	 * @return
	 */
	public boolean isClsKnown(String fullName) {
		return nameMap.containsKey(fullName);
	}

	/**
	 * Get the ClspClass for an object
	 *
	 * @param type the ArgType of the object to fetch
	 * @return
	 */
	public ClspClass getClsDetails(ArgType type) {
		return getClsDetails(type.getObject());
	}

	/**
	 * Get the ClspClass for a class string
	 *
	 * @param cls the name of the class to fetch
	 * @return
	 */
	public ClspClass getClsDetails(String cls) {
		return nameMap.get(cls);
	}

	@Nullable
	public IMethodDetails getMethodDetails(MethodInfo methodInfo) {
		ClspClass cls = nameMap.get(methodInfo.getDeclClass().getRawName());
		if (cls == null) {
			return null;
		}
		ClspMethod clspMethod = getMethodFromClass(cls, methodInfo);
		if (clspMethod != null) {
			return clspMethod;
		}
		// deep search
		for (ArgType parent : cls.getParents()) {
			ClspClass clspParent = getClspClass(parent);
			if (clspParent != null) {
				ClspMethod methodFromParent = getMethodFromClass(clspParent, methodInfo);
				if (methodFromParent != null) {
					return methodFromParent;
				}
			}
		}
		// unknown method
		return new SimpleMethodDetails(methodInfo);
	}

	private ClspMethod getMethodFromClass(ClspClass cls, MethodInfo methodInfo) {
		return cls.getMethodsMap().get(methodInfo.getShortId());
	}

	/**
	 * Add a class to the class path graph.
	 *
	 * Extracts the name, access flags, and parents information to store in a ClspClass entry
	 *
	 * @param cls
	 */
	private void addClass(ClassNode cls) {
		ArgType clsType = cls.getClassInfo().getType();
		String rawName = clsType.getObject();
		ClspClass clspClass = new ClspClass(clsType, -1, cls.getAccessFlags().rawValue(), ClspClassSource.APP);
		clspClass.setParents(ClsSet.makeParentsArray(cls));
		nameMap.put(rawName, clspClass);
	}

	/**
	 * @return {@code clsName} instanceof {@code implClsName}
	 */
	public boolean isImplements(String clsName, String implClsName) {
		Set<String> anc = getSuperTypes(clsName);
		return anc.contains(implClsName);
	}

	/**
	 * Get all implementations of a class
	 *
	 * @param clsName
	 * @return
	 */
	public List<String> getImplementations(String clsName) {
		List<String> list = implementsCache.get(clsName);
		return list == null ? Collections.emptyList() : list;
	}

	/**
	 * Get direct implementations of a class
	 *
	 * @param clsName
	 * @return
	 */
	public List<String> getChildren(String clsName) {
		List<String> list = immediateImplementsCache.get(clsName);
		return list == null ? Collections.emptyList() : list;
	}

	/**
	 * Propogate the immediate implements cache by reversing the immediateSuperTypesCache
	 */
	private void fillImmediateImplementsCache() {
		Map<String, List<String>> map = new HashMap<>(nameMap.size());
		List<String> classes = new ArrayList<>(nameMap.keySet());
		Collections.sort(classes);
		for (String cls : classes) {
			for (String st : getParents(cls)) {
				map.computeIfAbsent(st, v -> new ArrayList<>()).add(cls);
			}
		}
		immediateImplementsCache = map;
	}

	/**
	 * Propogate the implements cache by reversing the superTypesCache
	 */
	private void fillImplementsCache() {
		Map<String, List<String>> map = new HashMap<>(nameMap.size());
		List<String> classes = new ArrayList<>(nameMap.keySet());
		Collections.sort(classes);
		for (String cls : classes) {
			for (String st : getSuperTypes(cls)) {
				map.computeIfAbsent(st, v -> new ArrayList<>()).add(cls);
			}
		}
		implementsCache = map;
	}

	public String getCommonAncestor(String clsName, String implClsName) {
		if (clsName.equals(implClsName)) {
			return clsName;
		}
		ClspClass cls = nameMap.get(implClsName);
		if (cls == null) {
			missingClasses.add(clsName);
			return null;
		}
		if (isImplements(clsName, implClsName)) {
			return implClsName;
		}
		Set<String> anc = getSuperTypes(clsName);
		return searchCommonParent(anc, cls);
	}

	private String searchCommonParent(Set<String> anc, ClspClass cls) {
		for (ArgType p : cls.getParents()) {
			String name = p.getObject();
			if (anc.contains(name)) {
				return name;
			}
			ClspClass nCls = getClspClass(p);
			if (nCls != null) {
				String r = searchCommonParent(anc, nCls);
				if (r != null) {
					return r;
				}
			}
		}
		return null;
	}

	/**
	 * Get all super types for a class
	 *
	 * @param clsName
	 * @return
	 */
	public Set<String> getSuperTypes(String clsName) {
		Set<String> result = superTypesCache.get(clsName);
		return result == null ? Collections.emptySet() : result;
	}

	/**
	 * Get the immediate parents for a class
	 *
	 * @param clsName
	 * @return
	 */
	public Set<String> getParents(String clsName) {
		if (!immediateSuperTypesCache.containsKey(clsName)) {
			return null;
		}
		return immediateSuperTypesCache.get(clsName);
	}

	/**
	 * Propogate the immediateSuperTypesCache by traversing the nameMap and inspecting the parents of
	 * the ClspClasses
	 */
	private void fillImmediateSuperTypesCache() {
		Map<String, Set<String>> nametoSupertypesMap = new HashMap<>(nameMap.size());
		for (Map.Entry<String, ClspClass> entry : nameMap.entrySet()) {
			Set<String> supertypesSet = new HashSet<>();
			ClspClass cls = entry.getValue();
			addImmediateSuperTypes(cls, supertypesSet);
			nametoSupertypesMap.put(cls.getName(), supertypesSet);
		}
		immediateSuperTypesCache = nametoSupertypesMap;
	}

	/**
	 * Add only the names of immediate super types of cls to result
	 *
	 * @param cls
	 * @param result
	 */
	private void addImmediateSuperTypes(ClspClass cls, Set<String> result) {
		for (ArgType parentType : cls.getParents()) {
			if (parentType == null) {
				continue;
			}
			ClspClass parentCls = getClspClass(parentType);

			// add just the parent
			if (parentCls != null) {
				// this should be equivalent to parentType.getObject()
				result.add(parentCls.getName());
			} else {
				// parent type is unknown
				result.add(parentType.getObject());
			}
		}
	}

	/**
	 * Propogate the superTypesCache by traversing the immediateSuperTypesCache
	 */
	private void fillSuperTypesCache() {
		Map<String, Set<String>> nametoSupertypesMap = new HashMap<>(nameMap.size());
		for (String name : immediateSuperTypesCache.keySet()) {
			Set<String> supertypesSet = new HashSet<>();
			addSuperTypes(name, supertypesSet);
			nametoSupertypesMap.put(name, supertypesSet);
		}
		superTypesCache = nametoSupertypesMap;
	}

	/**
	 * Add the names of super types of cls to result using immediateSuperTypesCache
	 *
	 * @param clsName
	 * @param result
	 */
	private void addSuperTypes(String clsName, Set<String> result) {
		Set<String> parents = getParents(clsName);
		if (parents == null) {
			return;
		}

		for (String parent : parents) {
			// add the parent
			boolean isNew = result.add(parent);
			if (isNew) {
				// add super types of the parent
				addSuperTypes(parent, result);
			}
		}
	}

	/**
	 * Get the ClspClass for an object when propogating the super types cache.
	 * Adds objects to the missingClasses list if they are encountered but haven't been added to the
	 * nameMap.
	 *
	 * An internal equivalent to getClsDetails that handles constructing missing classes
	 *
	 * @param clsType
	 * @return
	 */
	@Nullable
	private ClspClass getClspClass(ArgType clsType) {
		ClspClass clspClass = nameMap.get(clsType.getObject());
		if (clspClass == null) {
			missingClasses.add(clsType.getObject());
		}
		return clspClass;
	}

	/**
	 * Display missing classes
	 */
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
