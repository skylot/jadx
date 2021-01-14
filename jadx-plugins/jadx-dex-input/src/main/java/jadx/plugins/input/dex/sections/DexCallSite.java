package jadx.plugins.input.dex.sections;

import java.util.List;

import jadx.api.plugins.input.data.ICallSite;
import jadx.api.plugins.input.data.IMethodHandle;
import jadx.api.plugins.input.data.IMethodRef;
import jadx.api.plugins.input.data.annotations.EncodedValue;

public class DexCallSite implements ICallSite {

	private final List<EncodedValue> values;

	public DexCallSite(List<EncodedValue> values) {
		this.values = values;
	}

	@Override
	public List<EncodedValue> getValues() {
		return values;
	}

	@Override
	public void load() {
		for (EncodedValue value : values) {
			Object obj = value.getValue();
			if (obj instanceof IMethodRef) {
				((IMethodRef) obj).load();
			} else if (obj instanceof IMethodHandle) {
				((IMethodHandle) obj).load();
			}
		}
	}

	@Override
	public String toString() {
		return "CallSite{" + values + '}';
	}
}
