package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.List;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.CallMthInterface;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.InvokeType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.PrimitiveType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.ssa.SSATransform;
import jadx.core.dex.visitors.typeinference.TypeInferenceVisitor;
import jadx.core.utils.InsnRemover;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "Constants Inline",
		desc = "Inline constant registers into instructions",
		runAfter = {
				SSATransform.class,
				MarkFinallyVisitor.class
		},
		runBefore = TypeInferenceVisitor.class
)
public class ConstInlineVisitor extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()) {
			return;
		}
		process(mth);
	}

	public static void process(MethodNode mth) {
		List<InsnNode> toRemove = new ArrayList<>();
		for (BlockNode block : mth.getBasicBlocks()) {
			toRemove.clear();
			for (InsnNode insn : block.getInstructions()) {
				checkInsn(mth, insn, toRemove);
			}
			InsnRemover.removeAllAndUnbind(mth, block, toRemove);
		}
	}

	private static void checkInsn(MethodNode mth, InsnNode insn, List<InsnNode> toRemove) {
		if (insn.contains(AFlag.DONT_INLINE)
				|| insn.contains(AFlag.DONT_GENERATE)
				|| insn.getResult() == null) {
			return;
		}

		SSAVar sVar = insn.getResult().getSVar();
		InsnArg constArg;

		InsnType insnType = insn.getType();
		if (insnType == InsnType.CONST || insnType == InsnType.MOVE) {
			constArg = insn.getArg(0);
			if (!constArg.isLiteral()) {
				return;
			}
			long lit = ((LiteralArg) constArg).getLiteral();
			if (lit == 0 && checkObjectInline(sVar)) {
				if (sVar.getUseCount() == 1) {
					InsnNode assignInsn = insn.getResult().getAssignInsn();
					if (assignInsn != null) {
						assignInsn.add(AFlag.DONT_INLINE);
					}
				}
				return;
			}
		} else if (insnType == InsnType.CONST_STR) {
			if (sVar.isUsedInPhi()) {
				return;
			}
			String s = ((ConstStringNode) insn).getString();
			FieldNode f = mth.getParentClass().getConstField(s);
			if (f == null) {
				InsnNode copy = insn.copy();
				copy.setResult(null);
				constArg = InsnArg.wrapArg(copy);
			} else {
				InsnNode constGet = new IndexInsnNode(InsnType.SGET, f.getFieldInfo(), 0);
				constArg = InsnArg.wrapArg(constGet);
				constArg.setType(ArgType.STRING);
			}
		} else {
			return;
		}
		if (checkForFinallyBlock(sVar)) {
			return;
		}

		// all check passed, run replace
		replaceConst(mth, insn, constArg, toRemove);
	}

	private static boolean checkForFinallyBlock(SSAVar sVar) {
		List<SSAVar> ssaVars = sVar.getCodeVar().getSsaVars();
		if (ssaVars.size() <= 1) {
			return false;
		}
		int countInsns = 0;
		int countFinallyInsns = 0;
		for (SSAVar ssaVar : ssaVars) {
			for (RegisterArg reg : ssaVar.getUseList()) {
				InsnNode parentInsn = reg.getParentInsn();
				if (parentInsn != null) {
					countInsns++;
					if (parentInsn.contains(AFlag.FINALLY_INSNS)) {
						countFinallyInsns++;
					}
				}
			}
		}
		return countFinallyInsns != 0 && countFinallyInsns != countInsns;
	}

	/**
	 * Don't inline null object if:
	 * - used as instance arg in invoke instruction
	 * - used in 'array.length'
	 */
	private static boolean checkObjectInline(SSAVar sVar) {
		for (RegisterArg useArg : sVar.getUseList()) {
			InsnNode insn = useArg.getParentInsn();
			if (insn == null) {
				continue;
			}
			InsnType insnType = insn.getType();
			if (insnType == InsnType.INVOKE) {
				InvokeNode inv = (InvokeNode) insn;
				if (inv.getInvokeType() != InvokeType.STATIC
						&& inv.getArg(0) == useArg) {
					return true;
				}
			} else if (insnType == InsnType.ARRAY_LENGTH) {
				if (insn.getArg(0) == useArg) {
					return true;
				}
			}
		}
		return false;
	}

	private static int replaceConst(MethodNode mth, InsnNode constInsn, InsnArg constArg, List<InsnNode> toRemove) {
		SSAVar ssaVar = constInsn.getResult().getSVar();
		List<RegisterArg> useList = new ArrayList<>(ssaVar.getUseList());
		int replaceCount = 0;
		for (RegisterArg arg : useList) {
			if (replaceArg(mth, arg, constArg, constInsn, toRemove)) {
				replaceCount++;
			}
		}
		if (replaceCount == useList.size()) {
			toRemove.add(constInsn);
		}
		return replaceCount;
	}

	private static boolean replaceArg(MethodNode mth, RegisterArg arg, InsnArg constArg, InsnNode constInsn, List<InsnNode> toRemove) {
		InsnNode useInsn = arg.getParentInsn();
		if (useInsn == null) {
			return false;
		}
		InsnType insnType = useInsn.getType();
		if (insnType == InsnType.PHI) {
			return false;
		}

		if (constArg.isLiteral()) {
			long literal = ((LiteralArg) constArg).getLiteral();
			ArgType argType = arg.getType();
			if (argType == ArgType.UNKNOWN) {
				argType = arg.getInitType();
			}
			if (argType.isObject() && literal != 0) {
				argType = ArgType.NARROW_NUMBERS;
			}
			LiteralArg litArg = InsnArg.lit(literal, argType);
			litArg.copyAttributesFrom(constArg);
			if (!useInsn.replaceArg(arg, litArg)) {
				return false;
			}
			// arg replaced, made some optimizations
			FieldNode fieldNode = null;
			ArgType litArgType = litArg.getType();
			if (litArgType.isTypeKnown()) {
				fieldNode = mth.getParentClass().getConstFieldByLiteralArg(litArg);
			} else if (litArgType.contains(PrimitiveType.INT)) {
				fieldNode = mth.getParentClass().getConstField((int) literal, false);
			}
			if (fieldNode != null) {
				litArg.wrapInstruction(mth, new IndexInsnNode(InsnType.SGET, fieldNode.getFieldInfo(), 0));
			} else {
				if (needExplicitCast(useInsn, litArg)) {
					litArg.add(AFlag.EXPLICIT_PRIMITIVE_TYPE);
				}
			}
		} else {
			if (!useInsn.replaceArg(arg, constArg.duplicate())) {
				return false;
			}
		}
		if (insnType == InsnType.RETURN) {
			useInsn.setSourceLine(constInsn.getSourceLine());
		}
		return true;
	}

	private static boolean needExplicitCast(InsnNode insn, LiteralArg arg) {
		if (insn instanceof CallMthInterface) {
			CallMthInterface callInsn = (CallMthInterface) insn;
			MethodInfo callMth = callInsn.getCallMth();
			int offset = callInsn.getFirstArgOffset();
			int argIndex = insn.getArgIndex(arg);
			ArgType argType = callMth.getArgumentsTypes().get(argIndex - offset);
			if (argType.isPrimitive()) {
				arg.setType(argType);
				return argType.equals(ArgType.BYTE);
			}
		}
		return false;
	}
}
