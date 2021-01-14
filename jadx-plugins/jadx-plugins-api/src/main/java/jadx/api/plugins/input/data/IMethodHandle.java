package jadx.api.plugins.input.data;

public interface IMethodHandle {

	MethodHandleType getType();

	IFieldData getFieldRef();

	IMethodRef getMethodRef();

	void load();
}
