package jadx.core.dex.visitors.typeinference;

import java.util.List;

import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;

public class PostTypeInference {

	private PostTypeInference() {
	}

	public static boolean process(MethodNode mth, InsnNode insn) {
		DexNode dex = mth.dex();
		switch (insn.getType()) {
			case CONST:
				RegisterArg res = insn.getResult();
				LiteralArg litArg = (LiteralArg) insn.getArg(0);
				if (res.getType().isObject()) {
					long lit = litArg.getLiteral();
					if (lit != 0) {
						// incorrect literal value for object
						ArgType type = lit == 1 ? ArgType.BOOLEAN : ArgType.INT;
						// can't merge with object -> force it
						litArg.setType(type);
						res.getSVar().setType(type);
						return true;
					}
				}
				return litArg.merge(dex, res);

			case MOVE: {
				boolean change = false;
				if (insn.getResult().merge(dex, insn.getArg(0))) {
					change = true;
				}
				if (insn.getArg(0).merge(dex, insn.getResult())) {
					change = true;
				}
				return change;
			}

			case AGET:
				return fixArrayTypes(dex, insn.getArg(0), insn.getResult());

			case APUT:
				return fixArrayTypes(dex, insn.getArg(0), insn.getArg(2));

			case IF: {
				boolean change = false;
				if (insn.getArg(1).merge(dex, insn.getArg(0))) {
					change = true;
				}
				if (insn.getArg(0).merge(dex, insn.getArg(1))) {
					change = true;
				}
				return change;
			}

			// check argument types for overloaded methods
			case INVOKE: {
				boolean change = false;
				InvokeNode inv = (InvokeNode) insn;
				MethodInfo callMth = inv.getCallMth();
				MethodNode node = mth.dex().resolveMethod(callMth);
				if (node != null && node.isArgsOverload()) {
					List<ArgType> args = callMth.getArgumentsTypes();
					int j = inv.getArgsCount() - 1;
					for (int i = args.size() - 1; i >= 0; i--) {
						ArgType argType = args.get(i);
						InsnArg insnArg = inv.getArg(j--);
						if (insnArg.isRegister() && !argType.equals(insnArg.getType())) {
							insnArg.setType(argType);
							change = true;
						}
					}
				}
				return change;
			}

			case CHECK_CAST: {
				ArgType castType = (ArgType) ((IndexInsnNode) insn).getIndex();
				RegisterArg result = insn.getResult();
				ArgType resultType = result.getType();
				// don't override generic types of same base class
				boolean skip = castType.isObject() && resultType.isObject()
						&& castType.getObject().equals(resultType.getObject());
				if (!skip) {
					// workaround for compiler bug (see TestDuplicateCast)
					result.getSVar().setType(castType);
				}
				return true;
			}

			case PHI:
			case MERGE: {
				ArgType type = insn.getResult().getType();
				if (!type.isTypeKnown()) {
					for (InsnArg arg : insn.getArguments()) {
						if (arg.getType().isTypeKnown()) {
							type = arg.getType();
							break;
						}
					}
				}
				boolean changed = false;
				if (updateType(insn.getResult(), type)) {
					changed = true;
				}
				for (int i = 0; i < insn.getArgsCount(); i++) {
					RegisterArg arg = (RegisterArg) insn.getArg(i);
					if (updateType(arg, type)) {
						changed = true;
					}
				}
				return changed;
			}

			default:
				break;
		}
		return false;
	}

	private static boolean updateType(RegisterArg arg, ArgType type) {
		ArgType prevType = arg.getType();
		if (prevType == null || !prevType.equals(type)) {
			arg.setType(type);
			return true;
		}
		return false;
	}

	private static boolean fixArrayTypes(DexNode dex, InsnArg array, InsnArg elem) {
		boolean change = false;
		if (!elem.getType().isTypeKnown() && elem.merge(dex, array.getType().getArrayElement())) {
			change = true;
		}
		if (!array.getType().isTypeKnown() && array.merge(dex, ArgType.array(elem.getType()))) {
			change = true;
		}
		return change;
	}
}
