package jadx.gui.utils.pkgs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JavaPackage;
import jadx.core.dex.info.PackageInfo;
import jadx.core.utils.ListUtils;
import jadx.core.utils.Utils;
import jadx.gui.JadxWrapper;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JPackage;
import jadx.gui.utils.JNodeCache;

public class PackageHelper {
	private static final Logger LOG = LoggerFactory.getLogger(PackageHelper.class);

	private static final Comparator<JClass> CLASS_COMPARATOR = Comparator.comparing(JClass::getName, String.CASE_INSENSITIVE_ORDER);
	private static final Comparator<JPackage> PKG_COMPARATOR = Comparator.comparing(JPackage::getName, String.CASE_INSENSITIVE_ORDER);

	private final JadxWrapper wrapper;
	private List<String> excludedPackages;
	private JNodeCache nodeCache;

	private final Map<PackageInfo, JPackage> pkgInfoMap = new HashMap<>();

	public PackageHelper(JadxWrapper wrapper) {
		this.wrapper = wrapper;
	}

	public List<JPackage> getRoots(boolean flatPackages) {
		excludedPackages = wrapper.getExcludedPackages();
		nodeCache = wrapper.getCache().getNodeCache();
		pkgInfoMap.clear();
		if (flatPackages) {
			return prepareFlatPackages();
		}
		long start = System.currentTimeMillis();
		List<JPackage> roots = prepareHierarchyPackages();
		if (LOG.isDebugEnabled()) {
			LOG.debug("Prepare hierarchy packages in {} ms", System.currentTimeMillis() - start);
		}
		return roots;
	}

	public List<JRenamePackage> getRenameNodes(JPackage pkg) {
		List<JRenamePackage> list = new ArrayList<>();
		PackageInfo pkgInfo = pkg.getPkg().getPkgNode().getAliasPkgInfo();
		Set<String> added = new HashSet<>();
		do {
			JPackage jPkg = pkgInfoMap.get(pkgInfo);
			if (jPkg != null) {
				JavaPackage javaPkg = jPkg.getPkg();
				String fullName = javaPkg.isDefault() ? JPackage.PACKAGE_DEFAULT_HTML_STR : javaPkg.getFullName();
				String name = jPkg.isSynthetic() || javaPkg.isParentRenamed() ? fullName : javaPkg.getName();
				JRenamePackage renamePkg = new JRenamePackage(javaPkg, javaPkg.getRawFullName(), fullName, name);
				if (added.add(fullName)) {
					list.add(renamePkg);
				}
			}
			pkgInfo = pkgInfo.getParentPkg();
		} while (pkgInfo != null);
		return list;
	}

	private List<JPackage> prepareFlatPackages() {
		List<JPackage> list = new ArrayList<>();
		for (JavaPackage javaPkg : wrapper.getPackages()) {
			if (javaPkg.isLeaf()) {
				JPackage pkg = buildJPackage(javaPkg, false);
				pkg.setName(javaPkg.getFullName());
				list.add(pkg);
				pkgInfoMap.put(javaPkg.getPkgNode().getAliasPkgInfo(), pkg);
			}
		}
		list.sort(PKG_COMPARATOR);
		return list;
	}

	private List<JPackage> prepareHierarchyPackages() {
		JPackage root = new JPackage(null, true, Collections.emptyList(), new ArrayList<>(), true);
		List<JavaPackage> packages = wrapper.getPackages();
		List<JPackage> jPackages = new ArrayList<>(packages.size());
		// create nodes for exists packages
		for (JavaPackage javaPkg : packages) {
			JPackage jPkg = buildJPackage(javaPkg, false);
			jPackages.add(jPkg);
			PackageInfo aliasPkgInfo = javaPkg.getPkgNode().getAliasPkgInfo();
			jPkg.setName(aliasPkgInfo.getName());
			pkgInfoMap.put(aliasPkgInfo, jPkg);
			if (aliasPkgInfo.isRoot()) {
				root.getSubPackages().add(jPkg);
			}
		}
		// link subpackages, create missing packages created by renames
		for (JPackage jPkg : jPackages) {
			if (jPkg.getPkg().isLeaf()) {
				buildLeafPath(jPkg, root, pkgInfoMap);
			}
		}
		List<JPackage> toMerge = new ArrayList<>();
		traverseMiddlePackages(root, toMerge);
		Utils.treeDfsVisit(root, JPackage::getSubPackages, v -> v.getSubPackages().sort(PKG_COMPARATOR));
		return root.getSubPackages();
	}

	private void buildLeafPath(JPackage jPkg, JPackage root, Map<PackageInfo, JPackage> pkgMap) {
		JPackage currentJPkg = jPkg;
		PackageInfo current = jPkg.getPkg().getPkgNode().getAliasPkgInfo();
		while (true) {
			current = current.getParentPkg();
			if (current == null) {
				break;
			}
			JPackage parentJPkg = pkgMap.get(current);
			if (parentJPkg == null) {
				parentJPkg = buildJPackage(currentJPkg.getPkg(), true);
				parentJPkg.setName(current.getName());
				pkgMap.put(current, parentJPkg);
				if (current.isRoot()) {
					root.getSubPackages().add(parentJPkg);
				}
			}
			List<JPackage> subPackages = parentJPkg.getSubPackages();
			String pkgName = currentJPkg.getName();
			if (ListUtils.noneMatch(subPackages, p -> p.getName().equals(pkgName))) {
				subPackages.add(currentJPkg);
			}
			currentJPkg = parentJPkg;
		}
	}

	private static void traverseMiddlePackages(JPackage pkg, List<JPackage> toMerge) {
		List<JPackage> subPackages = pkg.getSubPackages();
		int count = subPackages.size();
		for (int i = 0; i < count; i++) {
			JPackage subPackage = subPackages.get(i);
			JPackage replacePkg = mergeMiddlePackages(subPackage, toMerge);
			if (replacePkg != subPackage) {
				subPackages.set(i, replacePkg);
			}
			traverseMiddlePackages(replacePkg, toMerge);
		}
	}

	private static JPackage mergeMiddlePackages(JPackage jPkg, List<JPackage> merged) {
		List<JPackage> subPackages = jPkg.getSubPackages();
		if (subPackages.size() == 1 && jPkg.getClasses().isEmpty()) {
			merged.add(jPkg);
			JPackage endPkg = mergeMiddlePackages(subPackages.get(0), merged);
			merged.clear();
			return endPkg;
		}
		if (!merged.isEmpty()) {
			merged.add(jPkg);
			jPkg.setName(Utils.listToString(merged, ".", JPackage::getName));
		}
		return jPkg;
	}

	private JPackage buildJPackage(JavaPackage javaPkg, boolean synthetic) {
		boolean pkgEnabled = isPkgEnabled(javaPkg.getRawFullName(), excludedPackages);
		List<JClass> classes;
		if (synthetic) {
			classes = Collections.emptyList();
		} else {
			classes = Utils.collectionMap(javaPkg.getClasses(), nodeCache::makeFrom);
			classes.sort(CLASS_COMPARATOR);
		}
		return new JPackage(javaPkg, pkgEnabled, classes, new ArrayList<>(), synthetic);
	}

	private static boolean isPkgEnabled(String fullPkgName, List<String> excludedPackages) {
		return excludedPackages.isEmpty()
				|| excludedPackages.stream()
						.noneMatch(p -> fullPkgName.equals(p) || fullPkgName.startsWith(p + '.'));
	}
}
