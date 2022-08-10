package jadx.api.args;

public enum UserRenamesMappingsMode {

	/**
	 * Just read, user can save manually (default)
	 */
	READ,

	/**
	 * Read and autosave after every change
	 */
	READ_AND_AUTOSAVE_EVERY_CHANGE,

	/**
	 * Read and autosave before exiting the app or closing the project
	 */
	READ_AND_AUTOSAVE_BEFORE_CLOSING,

	/**
	 * Don't load and don't save
	 */
	IGNORE;

	public static UserRenamesMappingsMode getDefault() {
		return READ;
	}

	public boolean shouldRead() {
		return this != IGNORE;
	}

	public boolean shouldWrite() {
		return this == READ_AND_AUTOSAVE_EVERY_CHANGE || this == READ_AND_AUTOSAVE_BEFORE_CLOSING;
	}
}
