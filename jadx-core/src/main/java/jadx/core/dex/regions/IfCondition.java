package jadx.core.dex.regions;

import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.IfOp;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.BlockNode;

import java.util.Arrays;
import java.util.List;

public final class IfCondition {

	public static IfCondition fromIfBlock(BlockNode header) {
		if (header == null)
			return null;

		IfNode ifNode = (IfNode) header.getInstructions().get(0);
		return new IfCondition(new Compare(ifNode));
	}

	public static IfCondition not(IfCondition a) {
		return new IfCondition(MODE.NOT, Arrays.asList(a));
	}

	public static IfCondition and(IfCondition a, IfCondition b) {
		return new IfCondition(MODE.AND, Arrays.asList(a, b));
	}

	public static IfCondition or(IfCondition a, IfCondition b) {
		return new IfCondition(MODE.OR, Arrays.asList(a, b));
	}

	public static final class Compare {
		private final IfNode insn;

		public Compare(IfNode ifNode) {
			this.insn = ifNode;
		}

		public IfOp getOp() {
			return insn.getOp();
		}

		public InsnArg getA() {
			return insn.getArg(0);
		}

		public InsnArg getB() {
			if (insn.isZeroCmp())
				return InsnArg.lit(0, getA().getType());
			else
				return insn.getArg(1);
		}

		@Override
		public String toString() {
			return getA() + " " + getOp().getSymbol() + " " + getB();
		}
	}

	public static enum MODE {
		COMPARE,
		NOT,
		AND,
		OR
	}

	private final MODE mode;
	private final List<IfCondition> args;
	private final Compare compare;

	private IfCondition(Compare compare) {
		this.mode = MODE.COMPARE;
		this.compare = compare;
		this.args = null;
	}

	private IfCondition(MODE mode, List<IfCondition> args) {
		this.mode = mode;
		this.args = args;
		this.compare = null;
	}

	public MODE getMode() {
		return mode;
	}

	public List<IfCondition> getArgs() {
		return args;
	}

	public boolean isCompare() {
		return mode == MODE.COMPARE;
	}

	public Compare getCompare() {
		return compare;
	}

	@Override
	public String toString() {
		switch (mode) {
			case COMPARE:
				return compare.toString();
			case NOT:
				return "!" + args;
			case AND:
				return "&& " + args;
			case OR:
				return "|| " + args;
		}
		return "??";
	}
}
