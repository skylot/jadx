package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.AccessFlags;
import jadx.core.Consts;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.MethodInlineAttr;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
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
		if (canInline(mth)) {
			List<BlockNode> blocks = mth.getBasicBlocks();
			if (blocks == null) {
				return null;
			}
			if (blocks.size() == 2) {
				BlockNode returnBlock = blocks.get(1);
				if (returnBlock.contains(AFlag.RETURN) || returnBlock.getInstructions().isEmpty()) {
					MethodInlineAttr inlined = inlineMth(mth, blocks.get(0), returnBlock);
					if (inlined != null) {
						return inlined;
					}
				}
			}
		}
		return MethodInlineAttr.inlineNotNeeded(mth);
	}

	public static boolean canInline(MethodNode mth) {
		if (mth.isNoCode() || mth.contains(AFlag.DONT_GENERATE)) {
			return false;
		}
		AccessInfo accessFlags = mth.getAccessFlags();
		return accessFlags.isSynthetic() && accessFlags.isStatic();
	}

	@Nullable
	private static MethodInlineAttr inlineMth(MethodNode mth, BlockNode firstBlock, BlockNode returnBlock) {
		List<InsnNode> insnList = firstBlock.getInstructions();
		if (insnList.isEmpty()) {
			// synthetic field getter
			BlockNode block = mth.getBasicBlocks().get(1);
			InsnNode insn = block.getInstructions().get(0);
			// set arg from 'return' instruction
			return addInlineAttr(mth, InsnNode.wrapArg(insn.getArg(0)));
		}
		// synthetic field setter or method invoke
		if (insnList.size() == 1) {
			return addInlineAttr(mth, insnList.get(0));
		}
		// TODO: inline field arithmetics. Disabled tests: TestAnonymousClass3a and TestAnonymousClass5
		return null;
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
			MethodNode callMthNode = mth.root().deepResolveMethod(invoke.getCallMth());
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
			mth.addAttr(AType.COMMENTS, "JADX DEBUG: can't inline method, not implemented redirect type: " + insn);
		}
		return false;
	}
}
