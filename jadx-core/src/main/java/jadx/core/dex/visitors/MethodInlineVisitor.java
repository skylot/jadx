package jadx.core.dex.visitors;

import java.util.List;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.nodes.MethodInlineAttr;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.exceptions.JadxException;

/**
 * Inline synthetic methods.
 */
public class MethodInlineVisitor extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		AccessInfo accessFlags = mth.getAccessFlags();
		if (accessFlags.isSynthetic()
				&& accessFlags.isStatic()
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
				CodeShrinker.shrinkMethod(mth);

				insnList = firstBlock.getInstructions();
				if (insnList.size() == 1) {
					addInlineAttr(mth, insnList.get(0));
				}
			}
		}
	}

	private static void addInlineAttr(MethodNode mth, InsnNode insn) {
		mth.addAttr(new MethodInlineAttr(insn));
		mth.add(AFlag.DONT_GENERATE);
	}
}
