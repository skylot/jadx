package jadx.api.plugins.input.data.impl;

import jadx.api.plugins.input.data.IFieldData;
import jadx.api.plugins.input.data.IMethodHandle;
import jadx.api.plugins.input.data.IMethodRef;
import jadx.api.plugins.input.data.MethodHandleType;

public class FieldRefHandle implements IMethodHandle {

	private final IFieldData fieldRef;
	private final MethodHandleType type;

	public FieldRefHandle(MethodHandleType type, IFieldData fieldRef) {
		this.fieldRef = fieldRef;
		this.type = type;
	}

	@Override
	public MethodHandleType getType() {
		return type;
	}

	@Override
	public IFieldData getFieldRef() {
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
