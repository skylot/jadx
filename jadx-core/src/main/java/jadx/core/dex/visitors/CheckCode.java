package jadx.core.dex.visitors;

import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "CheckCode",
		desc = "Check and remove bad or incorrect code"
)
public class CheckCode extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		MethodInfo mthInfo = mth.getMethodInfo();
		if (mthInfo.getArgumentsTypes().size() > 255) {
			// java spec don't allow more than 255 args
			if (canRemoveMethod(mth)) {
				mth.ignoreMethod();
			} else {
				// TODO: convert args to array
			}
		}
	}

	private boolean canRemoveMethod(MethodNode mth) {
		if (mth.getUseIn().isEmpty()) {
			return true;
		}
		InsnNode[] insns = mth.getInstructions();
		if (insns.length == 0) {
			return true;
		}
		for (InsnNode insn : insns) {
			if (insn != null && insn.getType() != InsnType.NOP) {
				if (insn.getType() == InsnType.RETURN && insn.getArgsCount() == 0) {
					// ignore void return
				} else {
					// found useful instruction
					return false;
				}
			}
		}
		return true;
	}
}
