package jadx.api.plugins.input.data.impl;

import java.util.List;

import jadx.api.plugins.input.data.ICallSite;
import jadx.api.plugins.input.data.IMethodHandle;
import jadx.api.plugins.input.data.IMethodRef;
import jadx.api.plugins.input.data.annotations.EncodedValue;

public class CallSite implements ICallSite {

	private final List<EncodedValue> values;

	public CallSite(List<EncodedValue> values) {
		this.values = values;
	}

	@Override
	public void load() {
		for (EncodedValue value : values) {
			switch (value.getType()) {
				case ENCODED_METHOD_HANDLE:
					((IMethodHandle) value.getValue()).load();
					break;
				case ENCODED_METHOD:
					((IMethodRef) value.getValue()).load();
					break;
			}
		}
	}

	@Override
	public List<EncodedValue> getValues() {
		return values;
	}

	@Override
	public String toString() {
		return "CallSite{" + values + '}';
	}
}
