package jadx.core.dex.visitors.typeinference;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.InsnNode;

public class SelectTypeVisitor {

	private SelectTypeVisitor() {
	}

	public static void visit(DexNode dex, InsnNode insn) {
		InsnArg res = insn.getResult();
		if (res != null && !res.getType().isTypeKnown()) {
			selectType(dex, res);
		}
		for (InsnArg arg : insn.getArguments()) {
			if (!arg.getType().isTypeKnown()) {
				selectType(dex, arg);
			}
		}
	}

	private static void selectType(DexNode dex, InsnArg arg) {
		ArgType t = arg.getType();
		ArgType newType = ArgType.merge(dex, t, t.selectFirst());
		arg.setType(newType);
	}

}
