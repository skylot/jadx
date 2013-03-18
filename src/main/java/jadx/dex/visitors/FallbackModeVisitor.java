package jadx.dex.visitors;

import jadx.dex.attributes.AttributeType;
import jadx.dex.nodes.InsnNode;
import jadx.dex.nodes.MethodNode;
import jadx.dex.trycatch.CatchAttr;
import jadx.utils.exceptions.JadxException;

public class FallbackModeVisitor extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode())
			return;

		for (InsnNode insn : mth.getInstructions()) {
			// remove 'exception catch' for instruction which don't throw any exceptions
			CatchAttr catchAttr = (CatchAttr) insn.getAttributes().get(AttributeType.CATCH_BLOCK);
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
					case CMP_L:
					case CMP_G:
						catchAttr.getTryBlock().removeInsn(insn);
						break;

					default:
						break;
				}
			}
		}
	}
}
