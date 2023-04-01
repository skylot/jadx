package jadx.api.plugins.pass.types;

public class JadxPassType {
	private final String cls;

	public JadxPassType(String clsName) {
		this.cls = clsName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof JadxPassType)) {
			return false;
		}
		return cls.equals(((JadxPassType) o).cls);
	}

	@Override
	public int hashCode() {
		return cls.hashCode();
	}

	@Override
	public String toString() {
		return cls;
	}
}
