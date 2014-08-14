package jadx.core.dex.visitors;

import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.InstructionRemover;
import jadx.core.utils.exceptions.JadxException;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReSugarCode extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(ReSugarCode.class);

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()) {
			return;
		}
		InstructionRemover remover = new InstructionRemover(mth);
		for (BlockNode block : mth.getBasicBlocks()) {
			remover.setBlock(block);
			List<InsnNode> instructions = block.getInstructions();
			int size = instructions.size();
			for (int i = 0; i < size; i++) {
				InsnNode replacedInsn = process(mth, instructions, i, remover);
				if (replacedInsn != null) {
					instructions.set(i, replacedInsn);
				}
			}
			remover.perform();
		}
	}

	private static InsnNode process(MethodNode mth, List<InsnNode> instructions, int i, InstructionRemover remover) {
		InsnNode insn = instructions.get(i);
		switch (insn.getType()) {
			case NEW_ARRAY:
				return processNewArray(mth, instructions, i, remover);

			default:
				return null;
		}
	}

	/**
	 * Replace new array and sequence of array-put to new filled-array instruction.
	 */
	private static InsnNode processNewArray(MethodNode mth, List<InsnNode> instructions, int i, InstructionRemover remover) {
		InsnNode insn = instructions.get(i);
		InsnArg arg = insn.getArg(0);
		if (!arg.isLiteral()) {
			return null;
		}
		int len = (int) ((LiteralArg) arg).getLiteral();
		int size = instructions.size();
		if (len <= 0 || i + len >= size || instructions.get(i + len).getType() != InsnType.APUT) {
			return null;
		}
		InsnNode filledArr = new InsnNode(InsnType.FILLED_NEW_ARRAY, len);
		filledArr.setResult(insn.getResult());
		for (int j = 0; j < len; j++) {
			InsnNode put = instructions.get(i + 1 + j);
			if (put.getType() != InsnType.APUT) {
				LOG.debug("Not a APUT in expected new filled array: {}, method: {}", put, mth);
				return null;
			}
			filledArr.addArg(put.getArg(2));
			remover.add(put);
		}
		return filledArr;
	}
}
