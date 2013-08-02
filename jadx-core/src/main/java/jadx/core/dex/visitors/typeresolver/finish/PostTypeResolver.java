package jadx.core.dex.visitors.typeresolver.finish;

import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostTypeResolver {
	private static final Logger LOG = LoggerFactory.getLogger(PostTypeResolver.class);

	public static boolean visit(MethodNode mth, InsnNode insn) {
		switch (insn.getType()) {
			case CONST:
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

			case MOVE: {
				boolean change = false;
				if (insn.getResult().merge(insn.getArg(0)))
					change = true;
				if (insn.getArg(0).merge(insn.getResult()))
					change = true;
				return change;
			}

			case AGET:
				return fixArrayTypes(insn.getArg(0), insn.getResult());

			case APUT:
				return fixArrayTypes(insn.getArg(0), insn.getArg(2));

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
							insnArg.getTypedVar().forceSetType(argType);
							change = true;
						}
					}
				}
				return change;
			}

			default:
				break;
		}
		return false;

	}

	private static boolean fixArrayTypes(InsnArg array, InsnArg elem) {
		boolean change = false;
		if (!elem.getType().isTypeKnown() && elem.merge(array.getType().getArrayElement()))
			change = true;
		if (!array.getType().isTypeKnown() && array.merge(ArgType.array(elem.getType())))
			change = true;
		return change;
	}
}
