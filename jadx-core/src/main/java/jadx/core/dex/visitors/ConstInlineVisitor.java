package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.List;

import jadx.core.dex.attributes.AFlag;
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
import jadx.core.utils.InstructionRemover;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.exceptions.JadxOverflowException;

@JadxVisitor(
		name = "Constants Inline",
		desc = "Inline constant registers into instructions",
		runAfter = SSATransform.class,
		runBefore = TypeInferenceVisitor.class
)
public class ConstInlineVisitor extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()) {
			return;
		}
		List<InsnNode> toRemove = new ArrayList<>();
		for (BlockNode block : mth.getBasicBlocks()) {
			toRemove.clear();
			for (InsnNode insn : block.getInstructions()) {
				checkInsn(mth, insn, toRemove);
			}
			InstructionRemover.removeAll(mth, block, toRemove);
		}
	}

	private static void checkInsn(MethodNode mth, InsnNode insn, List<InsnNode> toRemove) {
		if (insn.contains(AFlag.DONT_INLINE)) {
			return;
		}
		InsnType insnType = insn.getType();
		if (insnType != InsnType.CONST && insnType != InsnType.MOVE) {
			return;
		}
		InsnArg arg = insn.getArg(0);
		if (!arg.isLiteral()) {
			return;
		}
		long lit = ((LiteralArg) arg).getLiteral();

		SSAVar sVar = insn.getResult().getSVar();
		if (lit == 0 && checkObjectInline(sVar)) {
			if (sVar.getUseCount() == 1) {
				InsnNode assignInsn = insn.getResult().getAssignInsn();
				if (assignInsn != null) {
					assignInsn.add(AFlag.DONT_INLINE);
				}
			}
			return;
		}
		replaceConst(mth, insn, lit, toRemove);
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

	private static void replaceConst(MethodNode mth, InsnNode constInsn, long literal, List<InsnNode> toRemove) {
		SSAVar ssaVar = constInsn.getResult().getSVar();
		List<RegisterArg> useList = new ArrayList<>(ssaVar.getUseList());
		int replaceCount = 0;
		for (RegisterArg arg : useList) {
			if (replaceArg(mth, arg, literal, constInsn, toRemove)) {
				replaceCount++;
			}
		}
		if (replaceCount == useList.size()) {
			toRemove.add(constInsn);
		}
	}

	private static boolean replaceArg(MethodNode mth, RegisterArg arg, long literal, InsnNode constInsn, List<InsnNode> toRemove) {
		InsnNode useInsn = arg.getParentInsn();
		if (useInsn == null) {
			return false;
		}
		InsnType insnType = useInsn.getType();
		if (insnType == InsnType.PHI || insnType == InsnType.MERGE) {
			return false;
		}
		ArgType argType = arg.getInitType();
		if (argType.isObject() && literal != 0) {
			argType = ArgType.NARROW_NUMBERS;
		}
		LiteralArg litArg = InsnArg.lit(literal, argType);
		if (!useInsn.replaceArg(arg, litArg)) {
			return false;
		}
		// arg replaced, made some optimizations
		litArg.setType(arg.getInitType());

		FieldNode fieldNode = null;
		ArgType litArgType = litArg.getType();
		if (litArgType.isTypeKnown()) {
			fieldNode = mth.getParentClass().getConstFieldByLiteralArg(litArg);
		} else if (litArgType.contains(PrimitiveType.INT)) {
			fieldNode = mth.getParentClass().getConstField((int) literal, false);
		}
		if (fieldNode != null) {
			litArg.wrapInstruction(new IndexInsnNode(InsnType.SGET, fieldNode.getFieldInfo(), 0));
		}

		if (insnType == InsnType.RETURN) {
			useInsn.setSourceLine(constInsn.getSourceLine());
		} else if (insnType == InsnType.MOVE) {
			try {
				replaceConst(mth, useInsn, literal, toRemove);
			} catch (StackOverflowError e) {
				throw new JadxOverflowException("Stack overflow at const inline visitor");
			}
		}
		return true;
	}
}
