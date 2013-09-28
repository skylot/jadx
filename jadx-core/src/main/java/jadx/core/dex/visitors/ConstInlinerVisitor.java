package jadx.core.dex.visitors;

import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.exceptions.JadxException;

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
			InsnArg arg = insn.getArg(0);
			if (arg.isLiteral()) {
				ArgType resType = insn.getResult().getType();
				// make sure arg has correct type
				if (!arg.getType().isTypeKnown()) {
					arg.merge(resType);
				}
				long lit = ((LiteralArg) arg).getLiteral();
				return replaceConst(mth, block, insn, lit);
			}
			// TODO process string and class const
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
				if (arg != insn.getResult() && !registerReassignOnPath(block, useBlock, insn)) {
					// in most cases type not equal arg.getType()
					// just set unknown type and run type fixer
					LiteralArg litArg = InsnArg.lit(literal, ArgType.UNKNOWN);
					if (useInsn.replaceArg(arg, litArg)) {
						fixTypes(mth, useInsn, litArg);
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
	private static void fixTypes(MethodNode mth, InsnNode insn, LiteralArg litArg) {
		switch (insn.getType()) {
			case CONST:
				insn.getArg(0).merge(insn.getResult());
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

			case IF: {
				IfNode ifnode = (IfNode) insn;
				if (!ifnode.isZeroCmp()) {
					InsnArg arg0 = insn.getArg(0);
					InsnArg arg1 = insn.getArg(1);
					if (arg0 == litArg) {
						arg0.merge(arg1);
					} else {
						arg1.merge(arg0);
					}
				}
				break;
			}
			case CMP_G:
			case CMP_L: {
				InsnArg arg0 = insn.getArg(0);
				InsnArg arg1 = insn.getArg(1);
				if (arg0 == litArg) {
					arg0.merge(arg1);
				} else {
					arg1.merge(arg0);
				}
				break;
			}

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

			case ARITH:
				litArg.merge(insn.getResult());
				break;

			case APUT:
			case AGET:
				if (litArg == insn.getArg(1)) {
					litArg.merge(ArgType.INT);
				}
				break;

			case NEW_ARRAY:
				litArg.merge(ArgType.INT);
				break;

			default:
				break;
		}
	}
}
