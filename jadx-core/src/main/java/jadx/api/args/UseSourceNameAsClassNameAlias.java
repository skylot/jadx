package jadx.api.args;

import jadx.core.utils.exceptions.JadxRuntimeException;

public enum UseSourceNameAsClassNameAlias {
	ALWAYS,
	IF_BETTER,
	NEVER;

	public static UseSourceNameAsClassNameAlias getDefault() {
		return NEVER;
	}

	/**
	 * @deprecated Use {@link UseSourceNameAsClassNameAlias} directly.
	 */
	@Deprecated
	public boolean toBoolean() {
		switch (this) {
			case IF_BETTER:
				return true;
			case NEVER:
				return false;
			case ALWAYS:
				throw new JadxRuntimeException("No match between " + this + " and boolean");
			default:
				throw new JadxRuntimeException("Unhandled strategy: " + this);
		}
	}

	/**
	 * @deprecated Use {@link UseSourceNameAsClassNameAlias} directly.
	 */
	@Deprecated
	public static UseSourceNameAsClassNameAlias create(boolean useSourceNameAsAlias) {
		return useSourceNameAsAlias ? IF_BETTER : NEVER;
	}
}
