package jadx.gui.model;

import jadx.api.JavaClass;

public class JClass {

	private final JavaClass cls;

	public JClass(JavaClass cls) {
		this.cls = cls;
	}

	public JavaClass getCls() {
		return cls;
	}

	@Override
	public String toString() {
		return cls.getShortName();
	}
}
