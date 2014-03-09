package jadx.core.dex.visitors;

import jadx.core.dex.instructions.ArithNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.exceptions.JadxException;

import java.util.Iterator;
import java.util.List;

public class PrepareForCodeGen extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		List<BlockNode> blocks = mth.getBasicBlocks();
		if (blocks == null) {
			return;
		}
		for (BlockNode block : blocks) {
			removeInstructions(block);
			modifyArith(block);
		}
	}

	private static void removeInstructions(BlockNode block) {
		Iterator<InsnNode> it = block.getInstructions().iterator();
		while (it.hasNext()) {
			InsnNode insn = it.next();
			switch (insn.getType()) {
				case NOP:
				case MONITOR_ENTER:
				case MONITOR_EXIT:
					it.remove();
					break;

				case CONSTRUCTOR:
					ConstructorInsn co = (ConstructorInsn) insn;
					if (co.isSelf()) {
						it.remove();
					}
					break;
			}
		}
	}

	private static void modifyArith(BlockNode block) {
		List<InsnNode> list = block.getInstructions();
		for (int i = 0; i < list.size(); i++) {
			InsnNode insn = list.get(i);
			if (insn.getType() == InsnType.ARITH) {
				ArithNode arith = (ArithNode) insn;
				RegisterArg res = arith.getResult();
				InsnArg arg = arith.getArg(0);
				if (res.equals(arg)) {
					ArithNode newArith = new ArithNode(arith.getOp(), res, arith.getArg(1));
					list.set(i, newArith);
				}
			}
		}
	}
}
