package jadx.core.dex.visitors.typeinference;

public class TypeUpdateFlags {

	private boolean allowWider;

	public TypeUpdateFlags allowWider() {
		this.allowWider = true;
		return this;
	}

	public boolean isAllowWider() {
		return allowWider;
	}
}
