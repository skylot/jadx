package jadx.core.utils.kotlin;

public class ClsAliasPair {
	private final String pkg;
	private final String name;

	public ClsAliasPair(String pkg, String name) {
		this.pkg = pkg;
		this.name = name;
	}

	public String getPkg() {
		return pkg;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return pkg + '.' + name;
	}
}
