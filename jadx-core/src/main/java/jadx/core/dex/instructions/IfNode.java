package jadx.core.dex.instructions;

import java.util.List;

import jadx.api.plugins.input.insns.InsnData;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.PrimitiveType;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.InsnUtils;

import static jadx.core.utils.BlockUtils.getBlockByOffset;
import static jadx.core.utils.BlockUtils.selectOther;

public class IfNode extends GotoNode {

	protected IfOp op;

	private BlockNode thenBlock;
	private BlockNode elseBlock;

	public IfNode(InsnData insn, IfOp op) {
		super(InsnType.IF, insn.getTarget(), 2);
		this.op = op;
		ArgType argType = narrowTypeByOp(op);
		addArg(InsnArg.reg(insn, 0, argType));
		if (insn.getRegsCount() == 1) {
			addArg(InsnArg.lit(0, argType));
		} else {
			addArg(InsnArg.reg(insn, 1, argType));
		}
	}

	public IfNode(IfOp op, int targetOffset, InsnArg arg1, InsnArg arg2) {
		this(op, targetOffset);
		addArg(arg1);
		addArg(arg2);
	}

	private IfNode(IfOp op, int targetOffset) {
		super(InsnType.IF, targetOffset, 2);
		this.op = op;
	}

	// change default types priority
	private static final ArgType WIDE_TYPE = ArgType.unknown(
			PrimitiveType.INT, PrimitiveType.BOOLEAN,
			PrimitiveType.OBJECT, PrimitiveType.ARRAY,
			PrimitiveType.BYTE, PrimitiveType.SHORT, PrimitiveType.CHAR);

	private static final ArgType NUMBERS_TYPE = ArgType.unknown(
			PrimitiveType.INT, PrimitiveType.BYTE, PrimitiveType.SHORT, PrimitiveType.CHAR);

	private static ArgType narrowTypeByOp(IfOp op) {
		if (op == IfOp.EQ || op == IfOp.NE) {
			return WIDE_TYPE;
		}
		return NUMBERS_TYPE;
	}

	public IfOp getOp() {
		return op;
	}

	public void invertCondition() {
		op = op.invert();
		BlockNode tmp = thenBlock;
		thenBlock = elseBlock;
		elseBlock = tmp;
	}

	/**
	 * Change 'a != false' to 'a == true'
	 */
	public void normalize() {
		if (getOp() == IfOp.NE && getArg(1).isFalse()) {
			changeCondition(IfOp.EQ, getArg(0), LiteralArg.litTrue());
		}
	}

	public void changeCondition(IfOp op, InsnArg arg1, InsnArg arg2) {
		this.op = op;
		setArg(0, arg1);
		setArg(1, arg2);
	}

	@Override
	public void initBlocks(BlockNode curBlock) {
		List<BlockNode> successors = curBlock.getSuccessors();
		thenBlock = getBlockByOffset(target, successors);
		if (successors.size() == 1) {
			elseBlock = thenBlock;
		} else {
			elseBlock = selectOther(thenBlock, successors);
		}
	}

	@Override
	public boolean replaceTargetBlock(BlockNode origin, BlockNode replace) {
		boolean replaced = false;
		if (thenBlock == origin) {
			thenBlock = replace;
			replaced = true;
		}
		if (elseBlock == origin) {
			elseBlock = replace;
			replaced = true;
		}
		return replaced;
	}

	public BlockNode getThenBlock() {
		return thenBlock;
	}

	public BlockNode getElseBlock() {
		return elseBlock;
	}

	@Override
	public int getTarget() {
		return thenBlock == null ? target : thenBlock.getStartOffset();
	}

	@Override
	public boolean isSame(InsnNode obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof IfNode) || !super.isSame(obj)) {
			return false;
		}
		IfNode other = (IfNode) obj;
		return op == other.op;
	}

	@Override
	public InsnNode copy() {
		IfNode copy = new IfNode(op, target);
		copy.thenBlock = thenBlock;
		copy.elseBlock = elseBlock;
		return copyCommonParams(copy);
	}

	@Override
	public String toString() {
		return InsnUtils.formatOffset(offset) + ": "
				+ InsnUtils.insnTypeToString(insnType)
				+ getArg(0) + ' ' + op.getSymbol() + ' ' + getArg(1)
				+ "  -> " + (thenBlock != null ? thenBlock : InsnUtils.formatOffset(target))
				+ attributesString();
	}
}
