package jadx.api.args;

/**
 * Resources original name source (for deobfuscation)
 */
public enum ResourceNameSource {

	/**
	 * Automatically select best name (default)
	 */
	AUTO,

	/**
	 * Force use resources provided names
	 */
	RESOURCES,

	/**
	 * Force use resources names from R class
	 */
	CODE,
}
