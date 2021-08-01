package jadx.api.plugins.input.data.attributes;

public interface IJadxAttribute {

	IJadxAttrType<? extends IJadxAttribute> getAttrType();

	/**
	 * Mark type to skip unloading on node unload
	 */
	default boolean keepLoaded() {
		return false;
	}

	default String toAttrString() {
		return this.toString();
	}
}
