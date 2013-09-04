package jadx.gui.treemodel;

import jadx.api.JavaPackage;
import jadx.gui.JadxWrapper;
import jadx.gui.utils.Utils;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JRoot extends DefaultMutableTreeNode implements JNode {

	private static final ImageIcon ROOT_ICON = Utils.openIcon("java_model_obj");

	private final JadxWrapper wrapper;

	private boolean flatPackages = false;

	public JRoot(JadxWrapper wrapper) {
		this.wrapper = wrapper;
		updateChilds();
	}

	@Override
	public void updateChilds() {
		removeAllChildren();
		if (flatPackages) {
			for (JavaPackage pkg : wrapper.getPackages()) {
				add(new JPackage(pkg));
			}
		} else {
			// build packages hierarchy
			List<JPackage> rootPkgs = getHierarchyPackages(wrapper.getPackages());
			for (JPackage jPackage : rootPkgs) {
				jPackage.updateChilds();
				add(jPackage);
			}
		}
	}

	List<JPackage> getHierarchyPackages(List<JavaPackage> packages) {
		Map<String, JPackage> pkgMap = new HashMap<String, JPackage>();
		for (JavaPackage pkg : packages) {
			addPackage(pkgMap, new JPackage(pkg));
		}
		// merge packages without classes
		boolean repeat;
		do {
			repeat = false;
			for (JPackage pkg : pkgMap.values()) {
				if (pkg.getInnerPackages().size() == 1 && pkg.getClasses().isEmpty()) {
					JPackage innerPkg = pkg.getInnerPackages().get(0);
					pkg.getInnerPackages().clear();
					pkg.getInnerPackages().addAll(innerPkg.getInnerPackages());
					pkg.getClasses().addAll(innerPkg.getClasses());
					pkg.setName(pkg.getName() + "." + innerPkg.getName());
					innerPkg.getInnerPackages().clear();
					innerPkg.getClasses().clear();

					repeat = true;
					pkgMap.remove(innerPkg.getName());
					break;
				}
			}
		} while (repeat);

		// remove empty packages
		for (Iterator<Map.Entry<String, JPackage>> it = pkgMap.entrySet().iterator(); it.hasNext(); ) {
			JPackage pkg = it.next().getValue();
			if (pkg.getInnerPackages().isEmpty() && pkg.getClasses().isEmpty()) {
				it.remove();
			}
		}
		// find root packages
		Set<JPackage> inners = new HashSet<JPackage>();
		for (JPackage pkg : pkgMap.values()) {
			inners.addAll(pkg.getInnerPackages());
		}
		List<JPackage> rootPkgs = new ArrayList<JPackage>();
		for (JPackage pkg : pkgMap.values()) {
			if (!inners.contains(pkg)) {
				rootPkgs.add(pkg);
			}
		}
		Collections.sort(rootPkgs);
		return rootPkgs;
	}

	private void addPackage(Map<String, JPackage> pkgs, JPackage pkg) {
		String pkgName = pkg.getName();
		JPackage replaced = pkgs.put(pkgName, pkg);
		if (replaced != null) {
			pkg.getInnerPackages().addAll(replaced.getInnerPackages());
			pkg.getClasses().addAll(replaced.getClasses());
		}
		int dot = pkgName.lastIndexOf('.');
		if (dot > 0) {
			String prevPart = pkgName.substring(0, dot);
			String shortName = pkgName.substring(dot + 1);
			pkg.setName(shortName);
			JPackage prevPkg = pkgs.get(prevPart);
			if (prevPkg == null) {
				prevPkg = new JPackage(prevPart);
				addPackage(pkgs, prevPkg);
			}
			prevPkg.getInnerPackages().add(pkg);
		}
	}

	public boolean isFlatPackages() {
		return flatPackages;
	}

	public void setFlatPackages(boolean flatPackages) {
		if (this.flatPackages != flatPackages) {
			this.flatPackages = flatPackages;
			updateChilds();
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
	public int getLine() {
		return 0;
	}

	@Override
	public String toString() {
		File file = wrapper.getOpenFile();
		return file != null ? file.getName() : "File not open";
	}
}
