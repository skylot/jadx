package jadx.api.plugins.input.data;

public interface IMethodHandle {

	MethodHandleType getType();

	IFieldRef getFieldRef();

	IMethodRef getMethodRef();

	void load();
}
