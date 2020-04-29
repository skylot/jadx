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
}
