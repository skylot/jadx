package jadx.core.dex.visitors.typeinference;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.InsnNode;

public class SelectTypeVisitor {

	public static void visit(InsnNode insn) {
		InsnArg res = insn.getResult();
		if (res != null && !res.getType().isTypeKnown()) {
			selectType(res);
		}
		for (InsnArg arg : insn.getArguments()) {
			if (!arg.getType().isTypeKnown()) {
				selectType(arg);
			}
		}
	}

	private static void selectType(InsnArg arg) {
		ArgType t = arg.getType();
		ArgType newType = ArgType.merge(t, t.selectFirst());
		arg.setType(newType);
	}

}
