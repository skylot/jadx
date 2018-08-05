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
				if (checkInsn(mth, insn)) {
					toRemove.add(insn);
				}
			}
			InstructionRemover.removeAll(mth, block, toRemove);
		}
	}

	private static boolean checkInsn(MethodNode mth, InsnNode insn) {
		if (insn.getType() != InsnType.CONST || insn.contains(AFlag.DONT_INLINE)) {
			return false;
		}
		InsnArg arg = insn.getArg(0);
		if (!arg.isLiteral()) {
			return false;
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
			return false;
		}
		return replaceConst(mth, insn, lit);
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

	private static boolean replaceConst(MethodNode mth, InsnNode constInsn, long literal) {
		SSAVar sVar = constInsn.getResult().getSVar();
		List<RegisterArg> use = new ArrayList<>(sVar.getUseList());
		int replaceCount = 0;
		for (RegisterArg arg : use) {
			InsnNode useInsn = arg.getParentInsn();
			if (useInsn == null
					|| useInsn.getType() == InsnType.PHI
					|| useInsn.getType() == InsnType.MERGE
			) {
				continue;
			}
			LiteralArg litArg;
			ArgType argType = arg.getType();
			if (argType.isObject() && literal != 0) {
				argType = ArgType.NARROW_NUMBERS;
			}
			if (use.size() == 1 || arg.isTypeImmutable()) {
				// arg used only in one place
				litArg = InsnArg.lit(literal, argType);
			} else if (useInsn.getType() == InsnType.MOVE
					&& !useInsn.getResult().getType().isTypeKnown()) {
				// save type for 'move' instructions (hard to find type in chains of 'move')
				litArg = InsnArg.lit(literal, argType);
			} else {
				// in most cases type not equal arg.getType()
				// just set unknown type and run type fixer
				litArg = InsnArg.lit(literal, ArgType.UNKNOWN);
			}
			if (useInsn.replaceArg(arg, litArg)) {
				litArg.setType(arg.getInitType());
				replaceCount++;
				if (useInsn.getType() == InsnType.RETURN) {
					useInsn.setSourceLine(constInsn.getSourceLine());
				}

				FieldNode f = null;
				ArgType litArgType = litArg.getType();
				if (litArgType.isTypeKnown()) {
					f = mth.getParentClass().getConstFieldByLiteralArg(litArg);
				} else if (litArgType.contains(PrimitiveType.INT)) {
					f = mth.getParentClass().getConstField((int) literal, false);
				}
				if (f != null) {
					litArg.wrapInstruction(new IndexInsnNode(InsnType.SGET, f.getFieldInfo(), 0));
				}
			}
		}
		return replaceCount == use.size();
	}
}
