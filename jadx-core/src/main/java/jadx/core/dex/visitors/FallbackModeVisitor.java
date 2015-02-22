package jadx.core.dex.visitors;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.trycatch.CatchAttr;
import jadx.core.utils.exceptions.JadxException;

public class FallbackModeVisitor extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()) {
			return;
		}
		for (InsnNode insn : mth.getInstructions()) {
			if (insn == null) {
				continue;
			}
			// remove 'exception catch' for instruction which don't throw any exceptions
			CatchAttr catchAttr = insn.get(AType.CATCH_BLOCK);
			if (catchAttr != null) {
				switch (insn.getType()) {
					case RETURN:
					case IF:
					case GOTO:
					case MOVE:
					case MOVE_EXCEPTION:
					case ARITH: // ??
					case NEG:
					case CONST:
					case CONST_STR:
					case CONST_CLASS:
					case CMP_L:
					case CMP_G:
						catchAttr.getTryBlock().removeInsn(mth, insn);
						break;

					default:
						break;
				}
			}
		}
	}
}
