package jadx.dex.visitors.typeresolver.finish;

import jadx.dex.instructions.IfNode;
import jadx.dex.instructions.args.ArgType;
import jadx.dex.instructions.args.InsnArg;
import jadx.dex.instructions.args.LiteralArg;
import jadx.dex.instructions.args.RegisterArg;
import jadx.dex.nodes.InsnNode;

public class PostTypeResolver {

	public static boolean visit(InsnNode insn) {
		switch (insn.getType()) {
			case CONST:
				if (insn.getArgsCount() > 0) {
					RegisterArg res = insn.getResult();
					LiteralArg litArg = (LiteralArg) insn.getArg(0);
					if (res.getType().isObject()) {
						long lit = litArg.getLiteral();
						if (lit != 0) {
							// incorrect literal value for object
							ArgType type = (lit == 1 ? ArgType.BOOLEAN : ArgType.INT);
							// can't merge with object -> force it
							litArg.getTypedVar().forceSetType(type);
							res.getTypedVar().forceSetType(type);
							return true;
						}
					}
					// return litArg.getTypedVar().forceSetType(res.getType());
					return litArg.merge(res);
				}
				break;

			case MOVE: {
				boolean change = false;
				if (insn.getResult().merge(insn.getArg(0)))
					change = true;
				if (insn.getArg(0).merge(insn.getResult()))
					change = true;
				return change;
			}

			case AGET: {
				boolean change = false;
				RegisterArg elem = insn.getResult();
				InsnArg array = insn.getArg(0);
				if (!elem.getType().isTypeKnown() && elem.merge(array.getType().getArrayElement()))
					change = true;
				if (!array.getType().isTypeKnown() && array.merge(ArgType.array(elem.getType())))
					change = true;
				return change;
			}

			case IF: {
				boolean change = false;
				IfNode ifnode = (IfNode) insn;
				if (!ifnode.isZeroCmp()) {
					if (insn.getArg(1).merge(insn.getArg(0)))
						change = true;
					if (insn.getArg(0).merge(insn.getArg(1)))
						change = true;
				}
				return change;
			}

			default:
				break;
		}
		return false;

	}
}
