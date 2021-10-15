package jadx.core.dex.instructions;

import java.util.List;

import jadx.api.plugins.input.data.ICallSite;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.insns.InsnData;
import jadx.core.dex.instructions.invokedynamic.CustomLambdaCall;
import jadx.core.dex.instructions.invokedynamic.CustomStringConcat;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.input.InsnDataUtils;

public class InvokeCustomBuilder {

	public static InsnNode build(MethodNode mth, InsnData insn, boolean isRange) {
		try {
			ICallSite callSite = InsnDataUtils.getCallSite(insn);
			if (callSite == null) {
				throw new JadxRuntimeException("Failed to get call site for insn: " + insn);
			}
			callSite.load();
			List<EncodedValue> values = callSite.getValues();
			if (CustomLambdaCall.isLambdaInvoke(values)) {
				return CustomLambdaCall.buildLambdaMethodCall(mth, insn, isRange, values);
			}
			if (CustomStringConcat.isStringConcat(values)) {
				return CustomStringConcat.buildStringConcat(insn, isRange, values);
			}
			// TODO: output raw dynamic call
			throw new JadxRuntimeException("Failed to process invoke-custom instruction: " + callSite);
		} catch (Exception e) {
			throw new JadxRuntimeException("'invoke-custom' instruction processing error: " + e.getMessage(), e);
		}
	}
}
