package jadx.dex.visitors;

import jadx.dex.info.FieldInfo;
import jadx.dex.instructions.IfNode;
import jadx.dex.instructions.IndexInsnNode;
import jadx.dex.instructions.InsnType;
import jadx.dex.instructions.InvokeNode;
import jadx.dex.instructions.args.ArgType;
import jadx.dex.instructions.args.InsnArg;
import jadx.dex.instructions.args.LiteralArg;
import jadx.dex.nodes.BlockNode;
import jadx.dex.nodes.InsnNode;
import jadx.dex.nodes.MethodNode;
import jadx.utils.BlockUtils;
import jadx.utils.exceptions.JadxException;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ConstInlinerVisitor extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode())
			return;

		for (BlockNode block : mth.getBasicBlocks()) {
			for (Iterator<InsnNode> it = block.getInstructions().iterator(); it.hasNext(); ) {
				InsnNode insn = it.next();
				if (checkInsn(mth, block, insn))
					it.remove();
			}
		}
	}

	private static boolean checkInsn(MethodNode mth, BlockNode block, InsnNode insn) {
		if (insn.getType() == InsnType.CONST) {
			if (insn.getArgsCount() == 1
					&& insn.getArg(0).isLiteral()
					&& insn.getResult().getType().getRegCount() == 1 /* process only narrow types */) {
				long lit = ((LiteralArg) insn.getArg(0)).getLiteral();
				return replaceConst(mth, block, insn, lit);
			}
			// TODO process string const
		}
		return false;
	}

	private static boolean replaceConst(MethodNode mth, BlockNode block, InsnNode insn, long literal) {
		List<InsnArg> use = insn.getResult().getTypedVar().getUseList();

		int replace = 0;
		for (InsnArg arg : use) {
			InsnNode useInsn = arg.getParentInsn();
			if (useInsn == null)
				continue;

			BlockNode useBlock = BlockUtils.getBlockByInsn(mth, useInsn);
			if (useBlock == block || useBlock.isDominator(block)) {
				if (arg != insn.getResult()
						&& !registerReassignOnPath(block, useBlock, insn)) {
					// in most cases type not equal arg.getType()
					// just set unknown type and run type fixer
					LiteralArg litArg = InsnArg.lit(literal, ArgType.NARROW);
					if (useInsn.replaceArg(arg, litArg)) {
						// if (useInsn.getType() == InsnType.MOVE) {
						// // 'move' became 'const'
						// InsnNode constInsn = new InsnNode(mth, InsnType.CONST, 1);
						// constInsn.setResult(useInsn.getResult());
						// constInsn.addArg(litArg);
						// ModVisitor.replaceInsn(useBlock, useInsn, constInsn);
						// fixTypes(mth, constInsn);
						// }
						fixTypes(mth, useInsn);
						replace++;
					}
				}
			}
		}
		return (replace + 1) == use.size();
	}

	private static boolean registerReassignOnPath(BlockNode block, BlockNode useBlock, InsnNode assignInsn) {
		if (block == useBlock)
			return false;

		Set<BlockNode> blocks = BlockUtils.getAllPathsBlocks(block, useBlock);
		// TODO store list of assign insn for each register
		int regNum = assignInsn.getResult().getRegNum();
		for (BlockNode b : blocks) {
			for (InsnNode insn : b.getInstructions()) {
				if (insn.getResult() != null
						&& insn != assignInsn
						&& insn.getResult().getRegNum() == regNum)
					return true;
			}
		}
		return false;
	}

	/**
	 * This is method similar to PostTypeResolver.visit method,
	 * but contains some expensive operations needed only after constant inline
	 */
	private static void fixTypes(MethodNode mth, InsnNode insn) {
		switch (insn.getType()) {
			case CONST:
				if (insn.getArgsCount() > 0) {
					insn.getArg(0).merge(insn.getResult());
				}
				break;

			case MOVE:
				insn.getResult().merge(insn.getArg(0));
				insn.getArg(0).merge(insn.getResult());
				break;

			case IPUT:
			case SPUT: {
				IndexInsnNode node = (IndexInsnNode) insn;
				insn.getArg(0).merge(((FieldInfo) node.getIndex()).getType());
				break;
			}

			case IF:
				IfNode ifnode = (IfNode) insn;
				if (!ifnode.isZeroCmp()) {
					insn.getArg(1).merge(insn.getArg(0));
					insn.getArg(0).merge(insn.getArg(1));
				}
				break;

			case RETURN:
				if (insn.getArgsCount() != 0) {
					insn.getArg(0).merge(mth.getReturnType());
				}
				break;

			case INVOKE:
				InvokeNode inv = (InvokeNode) insn;
				List<ArgType> types = inv.getCallMth().getArgumentsTypes();
				int count = insn.getArgsCount();
				int k = (types.size() == count ? 0 : -1);
				for (int i = 0; i < count; i++) {
					InsnArg arg = insn.getArg(i);
					if (!arg.getType().isTypeKnown()) {
						ArgType type;
						if (k >= 0)
							type = types.get(k);
						else
							type = mth.getParentClass().getClassInfo().getType();
						arg.merge(type);
					}
					k++;
				}
				break;

			default:
				break;
		}
	}
}
