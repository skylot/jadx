package jadx.api.plugins.input.data.impl;

import jadx.api.plugins.input.data.IFieldData;
import jadx.api.plugins.input.data.IMethodHandle;
import jadx.api.plugins.input.data.IMethodRef;
import jadx.api.plugins.input.data.MethodHandleType;

public class MethodRefHandle implements IMethodHandle {

	private final IMethodRef methodRef;
	private final MethodHandleType type;

	public MethodRefHandle(MethodHandleType type, IMethodRef methodRef) {
		this.methodRef = methodRef;
		this.type = type;
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
