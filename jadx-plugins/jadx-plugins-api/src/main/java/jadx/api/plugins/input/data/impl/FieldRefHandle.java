package jadx.api.plugins.input.data.impl;

import jadx.api.plugins.input.data.IFieldRef;
import jadx.api.plugins.input.data.IMethodHandle;
import jadx.api.plugins.input.data.IMethodRef;
import jadx.api.plugins.input.data.MethodHandleType;

public class FieldRefHandle implements IMethodHandle {

	private final IFieldRef fieldRef;
	private final MethodHandleType type;

	public FieldRefHandle(MethodHandleType type, IFieldRef fieldRef) {
		this.fieldRef = fieldRef;
		this.type = type;
	}

	@Override
	public MethodHandleType getType() {
		return type;
	}

	@Override
	public IFieldRef getFieldRef() {
		return fieldRef;
	}

	@Override
	public IMethodRef getMethodRef() {
		return null;
	}

	@Override
	public void load() {
		// already loaded
	}

	@Override
	public String toString() {
		return type + ": " + fieldRef;
	}
}
