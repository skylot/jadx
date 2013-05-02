package jadx.dex.visitors;

import jadx.dex.attributes.AttributeFlag;
import jadx.dex.attributes.IAttribute;
import jadx.dex.attributes.MethodInlineAttr;
import jadx.dex.instructions.InsnType;
import jadx.dex.nodes.BlockNode;
import jadx.dex.nodes.InsnNode;
import jadx.dex.nodes.MethodNode;
import jadx.utils.exceptions.JadxException;

public class MethodInlinerVisitor extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.getAccessFlags().isSynthetic() && mth.getAccessFlags().isStatic()) {
			if (mth.getBasicBlocks().size() == 1) {
				BlockNode block = mth.getBasicBlocks().get(0);
				// synthetic field getter
				if (block.getInstructions().size() == 1) {
					InsnNode insn = block.getInstructions().get(0);
					if (insn.getType() == InsnType.RETURN) {
						InsnNode inl = new InsnNode(InsnType.ARGS, 1);
						inl.addArg(insn.getArg(0));
						addInlineAttr(mth, inl);
						return;
					}
				}

				// synthetic field setter
				if (block.getInstructions().size() == 2) {
					if (block.getInstructions().get(1).getType() == InsnType.RETURN) {
						InsnNode insn = block.getInstructions().get(0);
						addInlineAttr(mth, insn);
						return;
					}
				}

				// synthetic method invoke
				if (block.getInstructions().size() == 1) {
					InsnNode insn = block.getInstructions().get(0);
					addInlineAttr(mth, insn);
                }
			}
		}
	}

	private static void addInlineAttr(MethodNode mth, InsnNode insn) {
		IAttribute attr = new MethodInlineAttr(insn);
		mth.getAttributes().add(attr);
		mth.getAttributes().add(AttributeFlag.DONT_GENERATE);
	}

}
