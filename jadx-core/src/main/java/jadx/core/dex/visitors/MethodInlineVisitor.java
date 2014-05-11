package jadx.core.dex.visitors;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.nodes.MethodInlineAttr;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.instructions.InsnType;
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
			BlockNode block = mth.getBasicBlocks().get(1);
			if (block.getInstructions().isEmpty()
					|| block.contains(AFlag.RETURN)) {
				inlineMth(mth);
			}
		}
	}

	private static void inlineMth(MethodNode mth) {
		BlockNode firstBlock = mth.getBasicBlocks().get(0);
		if (firstBlock.getInstructions().isEmpty()) {
			// synthetic field getter
			BlockNode block = mth.getBasicBlocks().get(1);
			InsnNode insn = block.getInstructions().get(0);
			InsnNode inl = new InsnNode(InsnType.ARGS, 1);
			// set arg from 'return' instruction
			inl.addArg(insn.getArg(0));
			addInlineAttr(mth, inl);
		} else {
			// synthetic field setter or method invoke
			if (firstBlock.getInstructions().size() == 1) {
				addInlineAttr(mth, firstBlock.getInstructions().get(0));
			}
		}
	}

	private static void addInlineAttr(MethodNode mth, InsnNode insn) {
		mth.addAttr(new MethodInlineAttr(insn));
		mth.add(AFlag.DONT_GENERATE);
	}
}
