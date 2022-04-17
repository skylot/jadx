package jadx.core.dex.visitors.typeinference;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.JadxVisitor;

@JadxVisitor(
		name = "Finish Type Inference",
		desc = "Check used types",
		runAfter = {
				TypeInferenceVisitor.class
		}
)
public final class FinishTypeInference extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode() || mth.getSVars().isEmpty()) {
			return;
		}
		mth.getSVars().forEach(var -> {
			ArgType type = var.getTypeInfo().getType();
			if (!type.isTypeKnown()) {
				mth.addWarnComment("Type inference failed for: " + var.getDetailedVarInfo(mth));
			}
			ArgType codeVarType = var.getCodeVar().getType();
			if (codeVarType == null) {
				var.getCodeVar().setType(ArgType.UNKNOWN);
			}
		});
	}
}
