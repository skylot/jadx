package jadx.gui.treemodel;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import jadx.api.JavaClass;
import jadx.api.JavaPackage;
import jadx.gui.utils.Utils;

public class JPackage extends JNode implements Comparable<JPackage> {
	private static final long serialVersionUID = -4120718634156839804L;

	private static final ImageIcon PACKAGE_ICON = Utils.openIcon("package_obj");

	private String name;
	private final List<JClass> classes;
	private final List<JPackage> innerPackages = new ArrayList<>(1);

	public JPackage(JavaPackage pkg) {
		this.name = pkg.getName();
		List<JavaClass> javaClasses = pkg.getClasses();
		this.classes = new ArrayList<>(javaClasses.size());
		for (JavaClass javaClass : javaClasses) {
			classes.add(new JClass(javaClass));
		}
		update();
	}

	public JPackage(String name) {
		this.name = name;
		this.classes = new ArrayList<>(1);
	}

	public final void update() {
		removeAllChildren();
		for (JPackage pkg : innerPackages) {
			pkg.update();
			add(pkg);
		}
		for (JClass cls : classes) {
			cls.update();
			add(cls);
		}
	}

	@Override
	public String getName() {
		return name;
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
}
