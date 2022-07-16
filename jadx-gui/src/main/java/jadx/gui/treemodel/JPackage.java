package jadx.gui.treemodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import jadx.api.JavaPackage;
import jadx.core.utils.Utils;
import jadx.gui.JadxWrapper;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.popupmenu.JPackagePopupMenu;
import jadx.gui.utils.UiUtils;

public class JPackage extends JNode {
	private static final long serialVersionUID = -4120718634156839804L;

	private static final ImageIcon PACKAGE_ICON = UiUtils.openSvgIcon("nodes/package");

	private String fullName;
	private String name;
	private boolean enabled;
	private List<JClass> classes;
	private List<JPackage> innerPackages;

	public JPackage(JavaPackage pkg, JadxWrapper wrapper) {
		this(pkg.getName(), pkg.getName(),
				isPkgEnabled(wrapper, pkg.getName()),
				Utils.collectionMap(pkg.getClasses(), JClass::new),
				new ArrayList<>());
		update();
	}

	public JPackage(String fullName, JadxWrapper wrapper) {
		this(fullName, fullName, isPkgEnabled(wrapper, fullName), new ArrayList<>(), new ArrayList<>());
	}

	public JPackage(String fullName, String name) {
		this(fullName, name, true, Collections.emptyList(), Collections.emptyList());
	}

	private JPackage(String fullName, String name, boolean enabled, List<JClass> classes, List<JPackage> innerPackages) {
		this.fullName = fullName;
		this.name = name;
		this.enabled = enabled;
		this.classes = classes;
		this.innerPackages = innerPackages;
	}

	private static boolean isPkgEnabled(JadxWrapper wrapper, String fullPkgName) {
		List<String> excludedPackages = wrapper.getExcludedPackages();
		return excludedPackages.isEmpty()
				|| excludedPackages.stream().filter(p -> !p.isEmpty())
						.noneMatch(p -> fullPkgName.equals(p) || fullPkgName.startsWith(p + '.'));
	}

	public final void update() {
		removeAllChildren();
		if (isEnabled()) {
			for (JPackage pkg : innerPackages) {
				pkg.update();
				add(pkg);
			}
			for (JClass cls : classes) {
				cls.update();
				add(cls);
			}
		}
	}

	@Override
	public JPopupMenu onTreePopupMenu(MainWindow mainWindow) {
		return new JPackagePopupMenu(mainWindow, this);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean canRename() {
		return true;
	}

	public String getFullName() {
		return fullName;
	}

	public void updateBothNames(String fullName, String name, JadxWrapper wrapper) {
		this.fullName = fullName;
		this.name = name;
		this.enabled = isPkgEnabled(wrapper, fullName);
	}

	public void updateName(String name) {
		this.name = name;
	}

	public List<JPackage> getInnerPackages() {
		return innerPackages;
	}

	public void setInnerPackages(List<JPackage> innerPackages) {
		this.innerPackages = innerPackages;
	}

	public List<JClass> getClasses() {
		return classes;
	}

	public void setClasses(List<JClass> classes) {
		this.classes = classes;
	}

	@Override
	public Icon getIcon() {
		return PACKAGE_ICON;
	}

	@Override
	public JClass getJParent() {
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		return name.equals(((JPackage) o).name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public String makeString() {
		return name;
	}

	@Override
	public String makeLongString() {
		return name;
	}

	public boolean isEnabled() {
		return enabled;
	}
}
