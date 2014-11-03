package jadx.core.dex.visitors;

import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.InvokeType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.InstructionRemover;
import jadx.core.utils.exceptions.JadxException;

import java.util.ArrayList;
import java.util.List;

public class ConstInlinerVisitor extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()) {
			return;
		}
		List<InsnNode> toRemove = new ArrayList<InsnNode>();
		for (BlockNode block : mth.getBasicBlocks()) {
			toRemove.clear();
			for (InsnNode insn : block.getInstructions()) {
				if (checkInsn(mth, insn)) {
					toRemove.add(insn);
				}
			}
			if (!toRemove.isEmpty()) {
				InstructionRemover.removeAll(mth, block, toRemove);
			}
		}
	}

	private static boolean checkInsn(MethodNode mth, InsnNode insn) {
		if (insn.getType() != InsnType.CONST) {
			return false;
		}
		InsnArg arg = insn.getArg(0);
		if (!arg.isLiteral()) {
			return false;
		}
		long lit = ((LiteralArg) arg).getLiteral();

		SSAVar sVar = insn.getResult().getSVar();
		if (lit == 0) {
			// don't inline null object if:
			// - used as instance arg in invoke instruction
			for (RegisterArg useArg : sVar.getUseList()) {
				InsnNode parentInsn = useArg.getParentInsn();
				if (parentInsn != null) {
					InsnType insnType = parentInsn.getType();
					if (insnType == InsnType.INVOKE) {
						InvokeNode inv = (InvokeNode) parentInsn;
						if (inv.getInvokeType() != InvokeType.STATIC
								&& inv.getArg(0) == useArg) {
							return false;
						}
					}
				}
			}
		}
		ArgType resType = insn.getResult().getType();
		// make sure arg has correct type
		if (!arg.getType().isTypeKnown()) {
			arg.merge(resType);
		}
		return replaceConst(mth, sVar, lit);
	}

	private static boolean replaceConst(MethodNode mth, SSAVar sVar, long literal) {
		List<RegisterArg> use = new ArrayList<RegisterArg>(sVar.getUseList());
		int replaceCount = 0;
		for (RegisterArg arg : use) {
//			if (arg.getSVar().isUsedInPhi()) {
//				continue;
//			}
			InsnNode useInsn = arg.getParentInsn();
			if (useInsn.getType() == InsnType.PHI) {
				continue;
			}
			LiteralArg litArg;
			if (use.size() == 1 || arg.isTypeImmutable()) {
				// arg used only in one place
				litArg = InsnArg.lit(literal, arg.getType());
			} else if (useInsn.getType() == InsnType.MOVE
					&& !useInsn.getResult().getType().isTypeKnown()) {
				// save type for 'move' instructions (hard to find type in chains of 'move')
				litArg = InsnArg.lit(literal, arg.getType());
			} else {
				// in most cases type not equal arg.getType()
				// just set unknown type and run type fixer
				litArg = InsnArg.lit(literal, ArgType.UNKNOWN);
			}
			if (useInsn.replaceArg(arg, litArg)) {
				fixTypes(mth, useInsn, litArg);
				replaceCount++;
			}
		}
		return replaceCount == use.size();
	}

	/**
	 * This is method similar to PostTypeInference.process method,
	 * but contains some expensive operations needed only after constant inline
	 */
	private static void fixTypes(MethodNode mth, InsnNode insn, LiteralArg litArg) {
		switch (insn.getType()) {
			case CONST:
				insn.getArg(0).merge(insn.getResult());
				break;

			case MOVE:
				insn.getResult().merge(insn.getArg(0));
				insn.getArg(0).merge(insn.getResult());
				break;

			case IPUT:
			case SPUT:
				IndexInsnNode node = (IndexInsnNode) insn;
				insn.getArg(0).merge(((FieldInfo) node.getIndex()).getType());
				break;

			case IF: {
				InsnArg arg0 = insn.getArg(0);
				InsnArg arg1 = insn.getArg(1);
				if (arg0 == litArg) {
					arg0.merge(arg1);
				} else {
					arg1.merge(arg0);
				}
				break;
			}
			case CMP_G:
			case CMP_L:
				InsnArg arg0 = insn.getArg(0);
				InsnArg arg1 = insn.getArg(1);
				if (arg0 == litArg) {
					arg0.merge(arg1);
				} else {
					arg1.merge(arg0);
				}
				break;

			case RETURN:
				if (insn.getArgsCount() != 0) {
					insn.getArg(0).merge(mth.getReturnType());
				}
				break;

			case INVOKE:
				InvokeNode inv = (InvokeNode) insn;
				List<ArgType> types = inv.getCallMth().getArgumentsTypes();
				int count = insn.getArgsCount();
				int k = types.size() == count ? 0 : -1;
				for (int i = 0; i < count; i++) {
					InsnArg arg = insn.getArg(i);
					if (!arg.getType().isTypeKnown()) {
						ArgType type;
						if (k >= 0) {
							type = types.get(k);
						} else {
							type = mth.getParentClass().getClassInfo().getType();
						}
						arg.merge(type);
					}
					k++;
				}
				break;

			case ARITH:
				litArg.merge(insn.getResult());
				break;

			case APUT:
			case AGET:
				if (litArg == insn.getArg(1)) {
					litArg.merge(ArgType.INT);
				}
				break;

			case NEW_ARRAY:
				litArg.merge(ArgType.INT);
				break;

			default:
				break;
		}
	}
}
