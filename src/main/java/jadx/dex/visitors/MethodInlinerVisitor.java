package jadx.dex.visitors;

import jadx.dex.attributes.AttributeFlag;
import jadx.dex.attributes.IAttribute;
import jadx.dex.attributes.MethodInlineAttr;
import jadx.dex.info.AccessInfo;
import jadx.dex.instructions.InsnType;
import jadx.dex.nodes.BlockNode;
import jadx.dex.nodes.InsnNode;
import jadx.dex.nodes.MethodNode;
import jadx.utils.exceptions.JadxException;

public class MethodInlinerVisitor extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		AccessInfo accessFlags = mth.getAccessFlags();
		if (accessFlags.isSynthetic() && accessFlags.isStatic()) {
			if (mth.getBasicBlocks().size() == 2) {
				BlockNode block = mth.getBasicBlocks().get(1);
				if (block.getAttributes().contains(AttributeFlag.RETURN)) {
					inlineMth(mth);
				}
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
		IAttribute attr = new MethodInlineAttr(insn);
		mth.getAttributes().add(attr);
		mth.getAttributes().add(AttributeFlag.DONT_GENERATE);
	}
}
