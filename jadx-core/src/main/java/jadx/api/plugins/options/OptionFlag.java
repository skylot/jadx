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
	 * Do not show this option in jadx-gui (useful if option is configured with custom ui)
	 */
	DISABLE_IN_GUI,

	/**
	 * Add this flag only if option do not affect generated code.
	 * If added, option value change will not cause code cache reset.
	 */
	NOT_CHANGING_CODE,
}
