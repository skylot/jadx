package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.AccessFlags;
import jadx.core.Consts;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.MethodInlineAttr;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "MarkMethodsForInline",
		desc = "Mark synthetic static methods for inline",
		runAfter = {
				FixAccessModifiers.class,
				ClassModifier.class
		}
)
public class MarkMethodsForInline extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		process(mth);
	}

	/**
	 * @return null if method can't be analyzed (not loaded)
	 */
	@Nullable
	public static MethodInlineAttr process(MethodNode mth) {
		MethodInlineAttr mia = mth.get(AType.METHOD_INLINE);
		if (mia != null) {
			return mia;
		}
		if (mth.contains(AFlag.METHOD_CANDIDATE_FOR_INLINE)) {
			if (mth.getBasicBlocks() == null) {
				return null;
			}
			MethodInlineAttr inlined = inlineMth(mth);
			if (inlined != null) {
				return inlined;
			}
		}
		return MethodInlineAttr.inlineNotNeeded(mth);
	}

	@Nullable
	private static MethodInlineAttr inlineMth(MethodNode mth) {
		List<InsnNode> insns = BlockUtils.collectInsnsWithLimit(mth.getBasicBlocks(), 2);
		int insnsCount = insns.size();
		if (insnsCount == 0) {
			return null;
		}
		if (insnsCount == 1) {
			InsnNode insn = insns.get(0);
			if (insn.getType() == InsnType.RETURN && insn.getArgsCount() == 1) {
				// synthetic field getter
				// set arg from 'return' instruction
				InsnArg arg = insn.getArg(0);
				if (!arg.isInsnWrap()) {
					return null;
				}
				return addInlineAttr(mth, ((InsnWrapArg) arg).getWrapInsn());
			}
			// method invoke
			return addInlineAttr(mth, insn);
		}
		if (insnsCount == 2 && insns.get(1).getType() == InsnType.RETURN) {
			InsnNode firstInsn = insns.get(0);
			InsnNode retInsn = insns.get(1);
			if (retInsn.getArgsCount() == 0
					|| isSyntheticAccessPattern(mth, firstInsn, retInsn)) {
				return addInlineAttr(mth, firstInsn);
			}
		}
		// TODO: inline field arithmetics. Disabled tests: TestAnonymousClass3a and TestAnonymousClass5
		return null;
	}

	private static boolean isSyntheticAccessPattern(MethodNode mth, InsnNode firstInsn, InsnNode retInsn) {
		List<RegisterArg> mthRegs = mth.getArgRegs();
		switch (firstInsn.getType()) {
			case IGET:
				return mthRegs.size() == 1
						&& retInsn.getArg(0).isSameVar(firstInsn.getResult())
						&& firstInsn.getArg(0).isSameVar(mthRegs.get(0));
			case SGET:
				return mthRegs.size() == 0
						&& retInsn.getArg(0).isSameVar(firstInsn.getResult());

			case IPUT:
				return mthRegs.size() == 2
						&& retInsn.getArg(0).isSameVar(mthRegs.get(1))
						&& firstInsn.getArg(0).isSameVar(mthRegs.get(1))
						&& firstInsn.getArg(1).isSameVar(mthRegs.get(0));
			case SPUT:
				return mthRegs.size() == 1
						&& retInsn.getArg(0).isSameVar(mthRegs.get(0))
						&& firstInsn.getArg(0).isSameVar(mthRegs.get(0));

			case INVOKE:
				return mthRegs.size() >= 1
						&& firstInsn.getArg(0).isSameVar(mthRegs.get(0))
						&& retInsn.getArg(0).isSameVar(firstInsn.getResult());
			default:
				return false;
		}
	}

	private static MethodInlineAttr addInlineAttr(MethodNode mth, InsnNode insn) {
		if (!fixVisibilityOfInlineCode(mth, insn)) {
			return null;
		}
		InsnNode copy = insn.copyWithoutResult();
		// unbind SSA variables from copy instruction
		List<RegisterArg> regArgs = new ArrayList<>();
		copy.getRegisterArgs(regArgs);
		for (RegisterArg regArg : regArgs) {
			copy.replaceArg(regArg, regArg.duplicate(regArg.getRegNum(), null));
		}
		return MethodInlineAttr.markForInline(mth, copy);
	}

	private static boolean fixVisibilityOfInlineCode(MethodNode mth, InsnNode insn) {
		int newVisFlag = AccessFlags.PUBLIC; // TODO: calculate more precisely
		InsnType insnType = insn.getType();
		if (insnType == InsnType.INVOKE) {
			InvokeNode invoke = (InvokeNode) insn;
			MethodNode callMthNode = mth.root().resolveMethod(invoke.getCallMth());
			if (callMthNode != null) {
				FixAccessModifiers.changeVisibility(callMthNode, newVisFlag);
			}
			return true;
		}
		if (insnType == InsnType.ONE_ARG) {
			InsnArg arg = insn.getArg(0);
			if (!arg.isInsnWrap()) {
				return false;
			}
			return fixVisibilityOfInlineCode(mth, ((InsnWrapArg) arg).getWrapInsn());
		}
		if (insn instanceof IndexInsnNode) {
			Object indexObj = ((IndexInsnNode) insn).getIndex();
			if (indexObj instanceof FieldInfo) {
				// field access must be already fixed in ModVisitor.fixFieldUsage method
				return true;
			}
		}
		if (Consts.DEBUG) {
			mth.addDebugComment("can't inline method, not implemented redirect type: " + insn);
		}
		return false;
	}
}
