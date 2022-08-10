package jadx.gui.treemodel;

import java.util.List;

import javax.swing.Icon;
import javax.swing.JPopupMenu;

import jadx.api.JavaNode;
import jadx.api.JavaPackage;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.popupmenu.JPackagePopupMenu;
import jadx.gui.utils.Icons;

import static jadx.gui.utils.UiUtils.escapeHtml;
import static jadx.gui.utils.UiUtils.fadeHtml;
import static jadx.gui.utils.UiUtils.wrapHtml;

public class JPackage extends JNode {
	private static final long serialVersionUID = -4120718634156839804L;

	public static final String PACKAGE_DEFAULT_HTML_STR = wrapHtml(fadeHtml(escapeHtml("<empty>")));

	private final JavaPackage pkg;
	private final boolean enabled;
	private final List<JClass> classes;
	private final List<JPackage> subPackages;

	/**
	 * Package created by full package alias, don't have a raw package reference.
	 * `pkg` field point to the closest raw package leaf.
	 */
	private final boolean synthetic;

	private String name;

	public JPackage(JavaPackage pkg, boolean enabled, List<JClass> classes, List<JPackage> subPackages, boolean synthetic) {
		this.pkg = pkg;
		this.enabled = enabled;
		this.classes = classes;
		this.subPackages = subPackages;
		this.synthetic = synthetic;
	}

	public void update() {
		removeAllChildren();
		if (isEnabled()) {
			for (JPackage pkg : subPackages) {
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

	public JavaPackage getPkg() {
		return pkg;
	}

	public JavaNode getJavaNode() {
		return pkg;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<JPackage> getSubPackages() {
		return subPackages;
	}

	public List<JClass> getClasses() {
		return classes;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isSynthetic() {
		return synthetic;
	}

	@Override
	public Icon getIcon() {
		return Icons.PACKAGE;
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
		return pkg.equals(((JPackage) o).pkg);
	}

	@Override
	public int hashCode() {
		return pkg.hashCode();
	}

	@Override
	public String makeString() {
		return name;
	}

	@Override
	public String makeStringHtml() {
		if (name.isEmpty()) {
			return PACKAGE_DEFAULT_HTML_STR;
		}
		return name;
	}

	@Override
	public String makeLongString() {
		return pkg.getFullName();
	}

	@Override
	public String toString() {
		return name;
	}
}
