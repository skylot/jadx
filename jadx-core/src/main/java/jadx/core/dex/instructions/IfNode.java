package jadx.core.dex.instructions;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.PrimitiveType;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.utils.InsnUtils;

import com.android.dx.io.instructions.DecodedInstruction;

import static jadx.core.utils.BlockUtils.getBlockByOffset;
import static jadx.core.utils.BlockUtils.selectOther;

public class IfNode extends GotoNode {

	protected boolean zeroCmp;
	protected IfOp op;

	private BlockNode thenBlock;
	private BlockNode elseBlock;

	public IfNode(int targ, InsnArg then, InsnArg els) {
		super(InsnType.IF, targ);
		addArg(then);
		if (els == null) {
			zeroCmp = true;
		} else {
			zeroCmp = false;
			addArg(els);
		}
	}

	public IfNode(DecodedInstruction insn, IfOp op) {
		super(InsnType.IF, insn.getTarget());
		this.op = op;

		ArgType type = ArgType.unknown(
				PrimitiveType.INT, PrimitiveType.OBJECT, PrimitiveType.ARRAY,
				PrimitiveType.BOOLEAN, PrimitiveType.SHORT, PrimitiveType.CHAR);

		addReg(insn, 0, type);
		if (insn.getRegisterCount() == 1) {
			zeroCmp = true;
		} else {
			zeroCmp = false;
			addReg(insn, 1, type);
		}
	}

	public IfOp getOp() {
		return op;
	}

	public boolean isZeroCmp() {
		return zeroCmp;
	}

	public void invertCondition() {
		op = op.invert();
		BlockNode tmp = thenBlock;
		thenBlock = elseBlock;
		elseBlock = tmp;
		target = thenBlock.getStartOffset();
	}

	public void changeCondition(InsnArg arg1, InsnArg arg2, IfOp op) {
		this.op = op;
		this.zeroCmp = arg2.isLiteral() && ((LiteralArg) arg2).getLiteral() == 0;
		setArg(0, arg1);
		if (!zeroCmp) {
			if (getArgsCount() == 2) {
				setArg(1, arg2);
			} else {
				addArg(arg2);
			}
		}
	}

	public void initBlocks(BlockNode curBlock) {
		thenBlock = getBlockByOffset(target, curBlock.getSuccessors());
		if (curBlock.getSuccessors().size() == 1) {
			elseBlock = thenBlock;
		} else {
			elseBlock = selectOther(thenBlock, curBlock.getSuccessors());
		}
		target = thenBlock.getStartOffset();
	}

	public BlockNode getThenBlock() {
		return thenBlock;
	}

	public BlockNode getElseBlock() {
		return elseBlock;
	}

	@Override
	public String toString() {
		return InsnUtils.formatOffset(offset) + ": "
				+ InsnUtils.insnTypeToString(insnType)
				+ getArg(0) + " " + op.getSymbol()
				+ " " + (zeroCmp ? "0" : getArg(1))
				+ "  -> " + (thenBlock != null ? thenBlock : InsnUtils.formatOffset(target));
	}
}
