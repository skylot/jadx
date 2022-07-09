package jadx.api.plugins.pass.types;

import jadx.api.plugins.pass.JadxPass;

public class JadxPassType {
	private final String cls;

	public JadxPassType(Class<? extends JadxPass> cls) {
		this.cls = cls.getSimpleName();
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
		return "JadxPassType{" + cls + '}';
	}
}
