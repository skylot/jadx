package jadx.api.args;

public enum GeneratedRenamesMappingFileMode {

	/**
	 * Load if found, don't save (default)
	 */
	READ,

	/**
	 * Load if found, save only if new (don't overwrite)
	 */
	READ_OR_SAVE,

	/**
	 * Don't load, always save
	 */
	OVERWRITE,

	/**
	 * Don't load and don't save
	 */
	IGNORE;

	public static GeneratedRenamesMappingFileMode getDefault() {
		return READ;
	}

	public boolean shouldRead() {
		return this == READ || this == READ_OR_SAVE;
	}

	public boolean shouldWrite() {
		return this == READ_OR_SAVE || this == OVERWRITE;
	}
}
