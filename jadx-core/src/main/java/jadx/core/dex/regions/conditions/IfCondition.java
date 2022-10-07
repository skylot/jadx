package jadx.core.dex.regions.conditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.attributes.AttrNode;
import jadx.core.dex.instructions.ArithNode;
import jadx.core.dex.instructions.ArithOp;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.IfOp;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;

public final class IfCondition extends AttrNode {

	public enum Mode {
		COMPARE,
		TERNARY,
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
		this.args = Collections.emptyList();
	}

	private IfCondition(Mode mode, List<IfCondition> args) {
		this.mode = mode;
		this.args = args;
		this.compare = null;
	}

	private IfCondition(IfCondition c) {
		this.mode = c.mode;
		this.compare = c.compare;
		if (c.mode == Mode.COMPARE) {
			this.args = Collections.emptyList();
		} else {
			this.args = new ArrayList<>(c.args);
		}
	}

	public static IfCondition fromIfBlock(BlockNode header) {
		InsnNode lastInsn = BlockUtils.getLastInsn(header);
		if (lastInsn == null) {
			return null;
		}
		return fromIfNode((IfNode) lastInsn);
	}

	public static IfCondition fromIfNode(IfNode insn) {
		return new IfCondition(new Compare(insn));
	}

	public static IfCondition ternary(IfCondition a, IfCondition b, IfCondition c) {
		return new IfCondition(Mode.TERNARY, Arrays.asList(a, b, c));
	}

	public static IfCondition merge(Mode mode, IfCondition a, IfCondition b) {
		if (a.getMode() == mode) {
			IfCondition n = new IfCondition(a);
			n.addArg(b);
			return n;
		}
		return new IfCondition(mode, Arrays.asList(a, b));
	}

	public Mode getMode() {
		return mode;
	}

	public List<IfCondition> getArgs() {
		return args;
	}

	public IfCondition first() {
		return args.get(0);
	}

	public IfCondition second() {
		return args.get(1);
	}

	public IfCondition third() {
		return args.get(2);
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

	public static IfCondition invert(IfCondition cond) {
		Mode mode = cond.getMode();
		switch (mode) {
			case COMPARE:
				return new IfCondition(cond.getCompare().invert());
			case TERNARY:
				return ternary(cond.first(), not(cond.second()), not(cond.third()));
			case NOT:
				return cond.first();
			case AND:
			case OR:
				List<IfCondition> args = cond.getArgs();
				List<IfCondition> newArgs = new ArrayList<>(args.size());
				for (IfCondition arg : args) {
					newArgs.add(invert(arg));
				}
				return new IfCondition(mode == Mode.AND ? Mode.OR : Mode.AND, newArgs);
		}
		throw new JadxRuntimeException("Unknown mode for invert: " + mode);
	}

	public static IfCondition not(IfCondition cond) {
		if (cond.getMode() == Mode.NOT) {
			return cond.first();
		}
		if (cond.getCompare() != null) {
			return new IfCondition(cond.compare.invert());
		}
		return new IfCondition(Mode.NOT, Collections.singletonList(cond));
	}

	public static IfCondition simplify(IfCondition cond) {
		if (cond.isCompare()) {
			Compare c = cond.getCompare();
			IfCondition i = simplifyCmpOp(c);
			if (i != null) {
				return i;
			}
			if (c.getOp() == IfOp.EQ && c.getB().isFalse()) {
				cond = new IfCondition(Mode.NOT, Collections.singletonList(new IfCondition(c.invert())));
			} else {
				c.normalize();
			}
		}
		List<IfCondition> args = null;
		for (int i = 0; i < cond.getArgs().size(); i++) {
			IfCondition arg = cond.getArgs().get(i);
			IfCondition simpl = simplify(arg);
			if (simpl != arg) {
				if (args == null) {
					args = new ArrayList<>(cond.getArgs());
				}
				args.set(i, simpl);
			}
		}
		if (args != null) {
			// arguments was changed
			cond = new IfCondition(cond.getMode(), args);
		}
		if (cond.getMode() == Mode.NOT && cond.first().getMode() == Mode.NOT) {
			cond = invert(cond.first());
		}
		if (cond.getMode() == Mode.TERNARY && cond.first().getMode() == Mode.NOT) {
			cond = invert(cond);
		}

		// for condition with a lot of negations => make invert
		if (cond.getMode() == Mode.OR || cond.getMode() == Mode.AND) {
			int count = cond.getArgs().size();
			if (count > 1) {
				int negCount = 0;
				for (IfCondition arg : cond.getArgs()) {
					if (arg.getMode() == Mode.NOT
							|| (arg.isCompare() && arg.getCompare().getOp() == IfOp.NE)) {
						negCount++;
					}
				}
				if (negCount > count / 2) {
					return not(invert(cond));
				}
			}
		}
		return cond;
	}

	private static IfCondition simplifyCmpOp(Compare c) {
		if (!c.getA().isInsnWrap()) {
			return null;
		}
		if (!c.getB().isLiteral()) {
			return null;
		}
		long lit = ((LiteralArg) c.getB()).getLiteral();
		if (lit != 0 && lit != 1) {
			return null;
		}

		InsnNode wrapInsn = ((InsnWrapArg) c.getA()).getWrapInsn();
		switch (wrapInsn.getType()) {
			case CMP_L:
			case CMP_G:
				if (lit == 0) {
					IfNode insn = c.getInsn();
					insn.changeCondition(insn.getOp(), wrapInsn.getArg(0), wrapInsn.getArg(1));
				}
				break;

			case ARITH:
				if (c.getB().getType() == ArgType.BOOLEAN) {
					ArithOp arithOp = ((ArithNode) wrapInsn).getOp();
					if (arithOp == ArithOp.OR || arithOp == ArithOp.AND) {
						IfOp ifOp = c.getInsn().getOp();
						boolean isTrue = ifOp == IfOp.NE && lit == 0
								|| ifOp == IfOp.EQ && lit == 1;

						IfOp op = isTrue ? IfOp.NE : IfOp.EQ;
						Mode mode = isTrue && arithOp == ArithOp.OR
								|| !isTrue && arithOp == ArithOp.AND ? Mode.OR : Mode.AND;

						IfNode if1 = new IfNode(op, -1, wrapInsn.getArg(0), LiteralArg.litFalse());
						IfNode if2 = new IfNode(op, -1, wrapInsn.getArg(1), LiteralArg.litFalse());
						return new IfCondition(mode,
								Arrays.asList(new IfCondition(new Compare(if1)),
										new IfCondition(new Compare(if2))));
					}
				}
				break;

			default:
				break;
		}

		return null;
	}

	public List<RegisterArg> getRegisterArgs() {
		List<RegisterArg> list = new ArrayList<>();
		if (mode == Mode.COMPARE) {
			compare.getInsn().getRegisterArgs(list);
		} else {
			for (IfCondition arg : args) {
				list.addAll(arg.getRegisterArgs());
			}
		}
		return list;
	}

	public void visitInsns(Consumer<InsnNode> visitor) {
		if (mode == Mode.COMPARE) {
			compare.getInsn().visitInsns(visitor);
		} else {
			args.forEach(arg -> arg.visitInsns(visitor));
		}
	}

	public List<InsnNode> collectInsns() {
		List<InsnNode> list = new ArrayList<>();
		visitInsns(list::add);
		return list;
	}

	public int getSourceLine() {
		for (InsnNode insn : collectInsns()) {
			int line = insn.getSourceLine();
			if (line != 0) {
				return line;
			}
		}
		return 0;
	}

	@Nullable
	public InsnNode getFirstInsn() {
		if (mode == Mode.COMPARE) {
			return compare.getInsn();
		}
		return args.get(0).getFirstInsn();
	}

	@Override
	public String toString() {
		switch (mode) {
			case COMPARE:
				return compare.toString();
			case TERNARY:
				return first() + " ? " + second() + " : " + third();
			case NOT:
				return "!(" + first() + ')';
			case AND:
			case OR:
				String op = mode == Mode.OR ? " || " : " && ";
				StringBuilder sb = new StringBuilder();
				sb.append('(');
				for (Iterator<IfCondition> it = args.iterator(); it.hasNext();) {
					IfCondition arg = it.next();
					sb.append(arg);
					if (it.hasNext()) {
						sb.append(op);
					}
				}
				sb.append(')');
				return sb.toString();
		}
		return "??";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof IfCondition)) {
			return false;
		}
		IfCondition other = (IfCondition) obj;
		if (mode != other.mode) {
			return false;
		}
		return Objects.equals(args, other.args)
				&& Objects.equals(compare, other.compare);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + mode.hashCode();
		result = 31 * result + args.hashCode();
		result = 31 * result + (compare != null ? compare.hashCode() : 0);
		return result;
	}
}
