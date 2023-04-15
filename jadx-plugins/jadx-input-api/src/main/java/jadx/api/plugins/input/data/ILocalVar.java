package jadx.api.plugins.input.data;

import org.jetbrains.annotations.Nullable;

public interface ILocalVar {
	String getName();

	int getRegNum();

	String getType();

	@Nullable
	String getSignature();

	int getStartOffset();

	int getEndOffset();

	/**
	 * Hint if variable is a method parameter.
	 * Can be incorrect and shouldn't be trusted.
	 */
	boolean isMarkedAsParameter();
}
