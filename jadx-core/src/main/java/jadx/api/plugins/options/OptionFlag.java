package jadx.api.plugins.options;

public enum OptionFlag {
	/**
	 * Store in project settings instead global (for jadx-gui)
	 */
	PER_PROJECT,

	/**
	 * Do not show this option in jadx-gui (useful if option is configured with custom ui)
	 */
	HIDE_IN_GUI,

	/**
	 * Option will be read-only in jadx-gui (can be used for calculated properties)
	 */
	DISABLE_IN_GUI,

	/**
	 * Add this flag only if the option does not affect generated code.
	 * If added, option value change will not cause code cache reset.
	 */
	NOT_CHANGING_CODE,
}
