package jadx.core.dex.instructions;

import java.util.List;

import com.android.dx.io.instructions.DecodedInstruction;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.PrimitiveType;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.InsnUtils;

import static jadx.core.utils.BlockUtils.getBlockByOffset;
import static jadx.core.utils.BlockUtils.selectOther;

public class IfNode extends GotoNode {

	// change default types priority
	private static final ArgType ARG_TYPE = ArgType.unknown(
			PrimitiveType.INT,
			PrimitiveType.OBJECT, PrimitiveType.ARRAY,
			PrimitiveType.BOOLEAN, PrimitiveType.BYTE, PrimitiveType.SHORT, PrimitiveType.CHAR);

	protected IfOp op;

	private BlockNode thenBlock;
	private BlockNode elseBlock;

	public IfNode(DecodedInstruction insn, IfOp op) {
		this(op, insn.getTarget(),
				InsnArg.reg(insn, 0, ARG_TYPE),
				insn.getRegisterCount() == 1 ? InsnArg.lit(0, ARG_TYPE) : InsnArg.reg(insn, 1, ARG_TYPE));
	}

	public IfNode(IfOp op, int targetOffset, InsnArg arg1, InsnArg arg2) {
		super(InsnType.IF, targetOffset, 2);
		this.op = op;
		addArg(arg1);
		addArg(arg2);
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
	public String toString() {
		return InsnUtils.formatOffset(offset) + ": "
				+ InsnUtils.insnTypeToString(insnType)
				+ getArg(0) + " " + op.getSymbol() + " " + getArg(1)
				+ "  -> " + (thenBlock != null ? thenBlock : InsnUtils.formatOffset(target));
	}
}
