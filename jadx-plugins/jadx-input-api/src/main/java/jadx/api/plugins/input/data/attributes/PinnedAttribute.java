package jadx.api.plugins.input.data.attributes;

public abstract class PinnedAttribute implements IJadxAttribute {

	@Override
	public final boolean keepLoaded() {
		return true;
	}
}
