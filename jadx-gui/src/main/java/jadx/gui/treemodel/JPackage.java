package jadx.gui.treemodel;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import org.jetbrains.annotations.NotNull;

import jadx.api.JavaClass;
import jadx.api.JavaPackage;
import jadx.gui.JadxWrapper;
import jadx.gui.utils.Utils;

public class JPackage extends JNode implements Comparable<JPackage> {
	private static final long serialVersionUID = -4120718634156839804L;

	private static final ImageIcon PACKAGE_ICON = Utils.openIcon("package_obj");

	private final String fullName;
	private String name;
	private boolean enabled;
	private final List<JClass> classes;
	private final List<JPackage> innerPackages = new ArrayList<>(1);

	public JPackage(JavaPackage pkg, JadxWrapper wrapper) {
		this.fullName = pkg.getName();
		this.name = pkg.getName();
		setEnabled(wrapper);
		List<JavaClass> javaClasses = pkg.getClasses();
		this.classes = new ArrayList<>(javaClasses.size());
		for (JavaClass javaClass : javaClasses) {
			classes.add(new JClass(javaClass));
		}
		update();
	}

	public JPackage(String name, JadxWrapper wrapper) {
		this.fullName = name;
		this.name = name;
		setEnabled(wrapper);
		this.classes = new ArrayList<>(1);
	}

	private void setEnabled(JadxWrapper wrapper) {
		List<String> excludedPackages = wrapper.getExcludedPackages();
		this.enabled = excludedPackages.isEmpty()
				|| excludedPackages.stream().filter(p -> !p.isEmpty())
						.noneMatch(p -> name.equals(p) || name.startsWith(p + '.'));
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
	public String getName() {
		return name;
	}

	public String getFullName() {
		return fullName;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<JPackage> getInnerPackages() {
		return innerPackages;
	}

	public List<JClass> getClasses() {
		return classes;
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
	public int getLine() {
		return 0;
	}

	@Override
	public int compareTo(@NotNull JPackage o) {
		return name.compareTo(o.name);
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
