package jadx.core.dex.visitors;

import jadx.core.dex.attributes.AttributeFlag;
import jadx.core.dex.instructions.ArithNode;
import jadx.core.dex.instructions.ArithOp;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.exceptions.JadxException;

import java.util.Iterator;
import java.util.List;

/**
 * Prepare instructions for code generation pass,
 * most of this modification breaks register dependencies,
 * so this pass must be just before CodeGen.
 */
public class PrepareForCodeGen extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		List<BlockNode> blocks = mth.getBasicBlocks();
		if (blocks == null) {
			return;
		}
		for (BlockNode block : blocks) {
			removeInstructions(block);
			checkInline(block);
			removeParenthesis(block);
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

	private static void checkInline(BlockNode block) {
		List<InsnNode> list = block.getInstructions();
		for (int i = 0; i < list.size(); i++) {
			InsnNode insn = list.get(i);
			// replace 'move' with inner wrapped instruction
			if (insn.getType() == InsnType.MOVE
					&& insn.getArg(0).isInsnWrap()
					&& !insn.getAttributes().contains(AttributeFlag.DECLARE_VAR)) {
				InsnNode wrapInsn = ((InsnWrapArg) insn.getArg(0)).getWrapInsn();
				wrapInsn.setResult(insn.getResult());
				list.set(i, wrapInsn);
			}
		}
	}

	private static void removeParenthesis(BlockNode block) {
		for (InsnNode insn : block.getInstructions()) {
			checkInsn(insn);
		}
	}

	/**
	 * Remove parenthesis for wrapped insn  in arith '+' or '-'
	 * ('(a + b) +c' => 'a + b + c')
	 */
	private static void checkInsn(InsnNode insn) {
		if (insn.getType() == InsnType.ARITH) {
			ArithNode arith = (ArithNode) insn;
			ArithOp op = arith.getOp();
			if (op == ArithOp.ADD || op == ArithOp.SUB) {
				for (int i = 0; i < 2; i++) {
					InsnArg arg = arith.getArg(i);
					if (arg.isInsnWrap()) {
						InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
						wrapInsn.getAttributes().add(AttributeFlag.DONT_WRAP);
						checkInsn(wrapInsn);
					}
				}
			}
		} else {
			for (InsnArg arg : insn.getArguments()) {
				if (arg.isInsnWrap()) {
					InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
					checkInsn(wrapInsn);
				}
			}
		}
	}

	/**
	 * Replace arithmetic operation with short form
	 * ('a = a + 2' => 'a += 2')
	 */
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
