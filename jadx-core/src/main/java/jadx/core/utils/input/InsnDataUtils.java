package jadx.core.utils.input;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.ICallSite;
import jadx.api.plugins.input.data.IMethodHandle;
import jadx.api.plugins.input.data.IMethodRef;
import jadx.api.plugins.input.data.annotations.EncodedType;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.insns.InsnData;
import jadx.api.plugins.input.insns.InsnIndexType;
import jadx.api.plugins.input.insns.custom.ICustomPayload;

public class InsnDataUtils {

	@Nullable
	public static ICallSite getCallSite(InsnData insnData) {
		if (insnData.getIndexType() != InsnIndexType.CALL_SITE) {
			return null;
		}
		ICustomPayload payload = insnData.getPayload();
		if (payload != null) {
			return ((ICallSite) payload);
		}
		return insnData.getIndexAsCallSite();
	}

	@Nullable
	public static IMethodRef getMethodRef(InsnData insnData) {
		if (insnData.getIndexType() != InsnIndexType.METHOD_REF) {
			return null;
		}
		ICustomPayload payload = insnData.getPayload();
		if (payload != null) {
			return ((IMethodRef) payload);
		}
		return insnData.getIndexAsMethod();
	}

	@Nullable
	public static IMethodHandle getMethodHandleAt(ICallSite callSite, int argNum) {
		if (callSite == null) {
			return null;
		}
		List<EncodedValue> values = callSite.getValues();
		if (argNum < values.size()) {
			EncodedValue encodedValue = values.get(argNum);
			if (encodedValue.getType() == EncodedType.ENCODED_METHOD_HANDLE) {
				return (IMethodHandle) encodedValue.getValue();
			}
		}
		return null;
	}
}
