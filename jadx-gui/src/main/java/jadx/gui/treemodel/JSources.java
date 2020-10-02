package jadx.gui.treemodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.*;

import jadx.api.JavaPackage;
import jadx.gui.JadxWrapper;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

public class JSources extends JNode {
	private static final long serialVersionUID = 8962924556824862801L;

	private static final ImageIcon ROOT_ICON = UiUtils.openIcon("packagefolder_obj");

	private final transient JadxWrapper wrapper;
	private final transient boolean flatPackages;

	public JSources(JRoot jRoot, JadxWrapper wrapper) {
		this.flatPackages = jRoot.isFlatPackages();
		this.wrapper = wrapper;
		update();
	}

	public final void update() {
		removeAllChildren();
		if (flatPackages) {
			for (JavaPackage pkg : wrapper.getPackages()) {
				add(new JPackage(pkg, wrapper));
			}
		} else {
			// build packages hierarchy
			List<JPackage> rootPkgs = getHierarchyPackages(wrapper.getPackages());
			for (JPackage jPackage : rootPkgs) {
				jPackage.update();
				add(jPackage);
			}
		}
	}

	/**
	 * Convert packages list to hierarchical packages representation
	 *
	 * @param packages input packages list
	 * @return root packages
	 */
	List<JPackage> getHierarchyPackages(List<JavaPackage> packages) {
		Map<String, JPackage> pkgMap = new HashMap<>();
		for (JavaPackage pkg : packages) {
			addPackage(pkgMap, new JPackage(pkg, wrapper));
		}
		// merge packages without classes
		boolean repeat;
		do {
			repeat = false;
			for (JPackage pkg : pkgMap.values()) {
				List<JPackage> innerPackages = pkg.getInnerPackages();
				if (innerPackages.size() == 1 && pkg.getClasses().isEmpty()) {
					JPackage innerPkg = innerPackages.get(0);
					pkg.setInnerPackages(innerPkg.getInnerPackages());
					pkg.setClasses(innerPkg.getClasses());
					String innerName = '.' + innerPkg.getName();
					pkg.updateBothNames(pkg.getFullName() + innerName, pkg.getName() + innerName, wrapper);

					innerPkg.setInnerPackages(Collections.emptyList());
					innerPkg.setClasses(Collections.emptyList());
					repeat = true;
					break;
				}
			}
		} while (repeat);

		// remove empty packages
		pkgMap.values().removeIf(pkg -> pkg.getInnerPackages().isEmpty() && pkg.getClasses().isEmpty());

		// use identity set for collect inner packages
		Set<JPackage> innerPackages = Collections.newSetFromMap(new IdentityHashMap<>());
		for (JPackage pkg : pkgMap.values()) {
			innerPackages.addAll(pkg.getInnerPackages());
		}
		// find root packages
		List<JPackage> rootPkgs = new ArrayList<>();
		for (JPackage pkg : pkgMap.values()) {
			if (!innerPackages.contains(pkg)) {
				rootPkgs.add(pkg);
			}
		}
		Collections.sort(rootPkgs);
		return rootPkgs;
	}

	private void addPackage(Map<String, JPackage> pkgs, JPackage pkg) {
		String pkgName = pkg.getFullName();
		JPackage replaced = pkgs.put(pkgName, pkg);
		if (replaced != null) {
			pkg.getInnerPackages().addAll(replaced.getInnerPackages());
			pkg.getClasses().addAll(replaced.getClasses());
		}
		int dot = pkgName.lastIndexOf('.');
		if (dot > 0) {
			String prevPart = pkgName.substring(0, dot);
			String shortName = pkgName.substring(dot + 1);
			pkg.updateName(shortName);
			JPackage prevPkg = pkgs.get(prevPart);
			if (prevPkg == null) {
				prevPkg = new JPackage(prevPart, wrapper);
				addPackage(pkgs, prevPkg);
			}
			prevPkg.getInnerPackages().add(pkg);
		}
	}

	@Override
	public Icon getIcon() {
		return ROOT_ICON;
	}

	@Override
	public JClass getJParent() {
		return null;
	}

	@Override
	public String makeString() {
		return NLS.str("tree.sources_title");
	}
}
