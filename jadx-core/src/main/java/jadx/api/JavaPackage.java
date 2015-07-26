package jadx.api;

import java.util.List;

import org.jetbrains.annotations.NotNull;

public final class JavaPackage implements JavaNode, Comparable<JavaPackage> {
	private final String name;
	private final List<JavaClass> classes;

	JavaPackage(String name, List<JavaClass> classes) {
		this.name = name;
		this.classes = classes;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getFullName() {
		// TODO: store full package name
		return name;
	}

	public List<JavaClass> getClasses() {
		return classes;
	}

	@Override
	public JavaClass getDeclaringClass() {
		return null;
	}

	@Override
	public JavaClass getTopParentClass() {
		return null;
	}

	@Override
	public int getDecompiledLine() {
		return 0;
	}

	@Override
	public int compareTo(@NotNull JavaPackage o) {
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
		JavaPackage that = (JavaPackage) o;
		return name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public String toString() {
		return name;
	}
}
