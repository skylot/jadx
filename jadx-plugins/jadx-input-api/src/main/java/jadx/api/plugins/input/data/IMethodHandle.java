package jadx.api.plugins.input.data;

import org.jetbrains.annotations.Nullable;

public interface IMethodHandle {

	MethodHandleType getType();

	@Nullable
	IFieldRef getFieldRef();

	@Nullable
	IMethodRef getMethodRef();

	void load();
}
