package jadx.core.dex.regions;

import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public final class IfCondition {

	public static IfCondition fromIfBlock(BlockNode header) {
		if (header == null) {
			return null;
		}
		return fromIfNode((IfNode) header.getInstructions().get(0));
	}

	public static IfCondition fromIfNode(IfNode insn) {
		return new IfCondition(new Compare(insn));
	}

	public static IfCondition merge(Mode mode, IfCondition a, IfCondition b) {
		if (a.getMode() == mode) {
			IfCondition n = new IfCondition(a);
			n.addArg(b);
			return n;
		} else if (b.getMode() == mode) {
			IfCondition n = new IfCondition(b);
			n.addArg(a);
			return n;
		} else {
			return new IfCondition(mode, Arrays.asList(a, b));
		}
	}

	public static enum Mode {
		COMPARE,
		NOT,
		AND,
		OR
	}

	private final Mode mode;
	private final List<IfCondition> args;
	private final Compare compare;

	private IfCondition(Compare compare) {
		this.mode = Mode.COMPARE;
		this.compare = compare;
		this.args = null;
	}

	private IfCondition(Mode mode, List<IfCondition> args) {
		this.mode = mode;
		this.args = args;
		this.compare = null;
	}

	private IfCondition(IfCondition c) {
		this.mode = c.mode;
		this.compare = c.compare;
		this.args = new ArrayList<IfCondition>(c.args);
	}

	public Mode getMode() {
		return mode;
	}

	public List<IfCondition> getArgs() {
		return args;
	}

	public void addArg(IfCondition c) {
		args.add(c);
	}

	public boolean isCompare() {
		return mode == Mode.COMPARE;
	}

	public Compare getCompare() {
		return compare;
	}

	public IfCondition invert() {
		switch (mode) {
			case COMPARE:
				return new IfCondition(compare.invert());
			case NOT:
				return new IfCondition(args.get(0));
			case AND:
			case OR:
				List<IfCondition> newArgs = new ArrayList<IfCondition>(args.size());
				for (IfCondition arg : args) {
					newArgs.add(arg.invert());
				}
				return new IfCondition(mode == Mode.AND ? Mode.OR : Mode.AND, newArgs);
		}
		throw new JadxRuntimeException("Unknown mode for invert: " + mode);
	}

	public List<RegisterArg> getRegisterArgs() {
		List<RegisterArg> list = new LinkedList<RegisterArg>();
		if (mode == Mode.COMPARE) {
			InsnArg a = compare.getA();
			if (a.isRegister()) {
				list.add((RegisterArg) a);
			}
			InsnArg b = compare.getA();
			if (a.isRegister()) {
				list.add((RegisterArg) b);
			}
		} else {
			for (IfCondition arg : args) {
				list.addAll(arg.getRegisterArgs());
			}
		}
		return list;
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
