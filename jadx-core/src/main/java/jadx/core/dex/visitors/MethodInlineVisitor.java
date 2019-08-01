package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.List;

import com.android.dx.rop.code.AccessFlags;

import jadx.core.Consts;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.MethodInlineAttr;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.shrink.CodeShrinkVisitor;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "InlineMethods",
		desc = "Inline synthetic static methods",
		runAfter = {
				FixAccessModifiers.class,
				ClassModifier.class
		}
)
public class MethodInlineVisitor extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode() || mth.contains(AFlag.DONT_GENERATE)) {
			return;
		}
		AccessInfo accessFlags = mth.getAccessFlags();
		if (accessFlags.isSynthetic() && accessFlags.isStatic()
				&& mth.getBasicBlocks().size() == 2) {
			BlockNode returnBlock = mth.getBasicBlocks().get(1);
			if (returnBlock.contains(AFlag.RETURN) || returnBlock.getInstructions().isEmpty()) {
				BlockNode firstBlock = mth.getBasicBlocks().get(0);
				inlineMth(mth, firstBlock, returnBlock);
			}
		}
	}

	private static void inlineMth(MethodNode mth, BlockNode firstBlock, BlockNode returnBlock) {
		List<InsnNode> insnList = firstBlock.getInstructions();
		if (insnList.isEmpty()) {
			// synthetic field getter
			BlockNode block = mth.getBasicBlocks().get(1);
			InsnNode insn = block.getInstructions().get(0);
			// set arg from 'return' instruction
			addInlineAttr(mth, InsnNode.wrapArg(insn.getArg(0)));
			return;
		}
		// synthetic field setter or method invoke
		if (insnList.size() == 1) {
			addInlineAttr(mth, insnList.get(0));
			return;
		}
		// other field operations
		if (insnList.size() == 2
				&& returnBlock.getInstructions().size() == 1
				&& !mth.getReturnType().equals(ArgType.VOID)) {
			InsnNode get = insnList.get(0);
			InsnNode put = insnList.get(1);
			InsnArg retArg = returnBlock.getInstructions().get(0).getArg(0);
			if (get.getType() == InsnType.IGET
					&& put.getType() == InsnType.IPUT
					&& retArg.isRegister()
					&& get.getResult().equalRegisterAndType((RegisterArg) retArg)) {
				RegisterArg retReg = (RegisterArg) retArg;
				retReg.getSVar().removeUse(retReg);
				CodeShrinkVisitor.shrinkMethod(mth);

				insnList = firstBlock.getInstructions();
				if (insnList.size() == 1) {
					addInlineAttr(mth, insnList.get(0));
				}
			}
		}
	}

	private static void addInlineAttr(MethodNode mth, InsnNode insn) {
		if (fixVisibilityOfInlineCode(mth, insn)) {
			if (Consts.DEBUG) {
				mth.addAttr(AType.COMMENTS, "Removed for inline");
			} else {
				InsnNode copy = insn.copy();
				// unbind SSA variables from copy instruction
				List<RegisterArg> regArgs = new ArrayList<>();
				copy.getRegisterArgs(regArgs);
				for (RegisterArg regArg : regArgs) {
					copy.replaceArg(regArg, regArg.duplicate(regArg.getRegNum(), null));
				}
				MethodInlineAttr.markForInline(mth, copy);
				mth.add(AFlag.DONT_GENERATE);
			}
		}
	}

	private static boolean fixVisibilityOfInlineCode(MethodNode mth, InsnNode insn) {
		int newVisFlag = AccessFlags.ACC_PUBLIC; // TODO: calculate more precisely
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
				FieldNode fieldNode = mth.root().deepResolveField(((FieldInfo) indexObj));
				if (fieldNode != null) {
					FixAccessModifiers.changeVisibility(fieldNode, newVisFlag);
				}
				return true;
			}
		}
		if (Consts.DEBUG) {
			mth.addAttr(AType.COMMENTS, "JADX DEBUG: can't inline method, not implemented redirect type: " + insn);
		}
		return false;
	}
}
