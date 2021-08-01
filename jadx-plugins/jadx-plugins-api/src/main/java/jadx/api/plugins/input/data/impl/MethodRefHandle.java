package jadx.api.plugins.input.data.impl;

import jadx.api.plugins.input.data.IFieldData;
import jadx.api.plugins.input.data.IMethodHandle;
import jadx.api.plugins.input.data.IMethodRef;
import jadx.api.plugins.input.data.MethodHandleType;

public class MethodRefHandle implements IMethodHandle {

	private final MethodHandleType type;
	private final IMethodRef methodRef;

	public MethodRefHandle(MethodHandleType type, IMethodRef methodRef) {
		this.type = type;
		this.methodRef = methodRef;
	}

	@Override
	public MethodHandleType getType() {
		return type;
	}

	@Override
	public IMethodRef getMethodRef() {
		return methodRef;
	}

	@Override
	public IFieldData getFieldRef() {
		return null;
	}

	@Override
	public void load() {
		methodRef.load();
	}

	@Override
	public String toString() {
		return type + ": " + methodRef;
	}
}
