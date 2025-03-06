package jadx.api.plugins.input.data.attributes;

/**
 * Jadx attribute: custom data container, can be added to most jadx nodes.
 */
public interface IJadxAttribute {

	IJadxAttrType<? extends IJadxAttribute> getAttrType();

	/**
	 * Mark type to skip unloading on node unload event
	 */
	default boolean keepLoaded() {
		return false;
	}

	default String toAttrString() {
		return this.toString();
	}
}
