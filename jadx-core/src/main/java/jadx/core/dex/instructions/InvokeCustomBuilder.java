package jadx.core.dex.instructions;

import java.util.List;

import jadx.api.plugins.input.data.ICallSite;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.insns.InsnData;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.JadxError;
import jadx.core.dex.instructions.invokedynamic.CustomLambdaCall;
import jadx.core.dex.instructions.invokedynamic.CustomRawCall;
import jadx.core.dex.instructions.invokedynamic.CustomStringConcat;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.Utils;
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
			try {
				return CustomRawCall.build(mth, insn, isRange, values);
			} catch (Exception e) {
				mth.addWarn("Failed to decode invoke-custom: \n" + Utils.listToString(values, "\n")
						+ ",\n exception: " + Utils.getStackTrace(e));
				InsnNode nop = new InsnNode(InsnType.NOP, 0);
				nop.add(AFlag.SYNTHETIC);
				nop.addAttr(AType.JADX_ERROR, new JadxError("Failed to decode invoke-custom: " + values, e));
				return nop;
			}
		} catch (Exception e) {
			throw new JadxRuntimeException("'invoke-custom' instruction processing error: " + e.getMessage(), e);
		}
	}
}
