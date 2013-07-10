package jadx.api;

import java.util.List;

public final class JavaPackage implements Comparable<JavaPackage> {
	private final String name;
	private final List<JavaClass> classes;

	JavaPackage(String name, List<JavaClass> classes) {
		this.name = name;
		this.classes = classes;
	}

	public String getName() {
		return name;
	}

	public List<JavaClass> getClasses() {
		return classes;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int compareTo(JavaPackage o) {
		return name.compareTo(o.name);
	}
}
