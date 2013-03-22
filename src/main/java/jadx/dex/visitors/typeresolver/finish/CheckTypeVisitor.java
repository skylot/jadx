package jadx.dex.visitors.typeresolver.finish;

import jadx.dex.instructions.args.InsnArg;
import jadx.dex.nodes.InsnNode;
import jadx.dex.nodes.MethodNode;
import jadx.utils.ErrorsCounter;

public class CheckTypeVisitor {

	public static void visit(MethodNode mth, InsnNode insn) {
		if (insn.getResult() != null) {
			if (!insn.getResult().getType().isTypeKnown()) {
				error("Wrong return type", mth, insn);
				return;
			}
		}

		for (InsnArg arg : insn.getArguments()) {
			if (!arg.getType().isTypeKnown()) {
				error("Wrong type", mth, insn);
				return;
			}
		}
	}

	private static void error(String msg, MethodNode mth, InsnNode insn) {
		ErrorsCounter.methodError(mth, msg + ": " + insn);
	}
}
