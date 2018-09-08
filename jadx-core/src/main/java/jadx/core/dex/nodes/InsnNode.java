package jadx.core.dex.nodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.android.dx.io.instructions.DecodedInstruction;
import com.rits.cloning.Cloner;

import jadx.core.dex.attributes.nodes.LineAttrNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.NamedArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.Utils;

public class InsnNode extends LineAttrNode {

	private static final Cloner INSN_CLONER = new Cloner();

	static {
		INSN_CLONER.dontClone(ArgType.class, SSAVar.class, LiteralArg.class, NamedArg.class);
		INSN_CLONER.dontCloneInstanceOf(RegisterArg.class);
	}

	protected final InsnType insnType;

	private RegisterArg result;
	private final List<InsnArg> arguments;
	protected int offset;

	public InsnNode(InsnType type, int argsCount) {
		this.insnType = type;
		this.offset = -1;

		if (argsCount == 0) {
			this.arguments = Collections.emptyList();
		} else {
			this.arguments = new ArrayList<>(argsCount);
		}
	}

	public static InsnNode wrapArg(InsnArg arg) {
		InsnNode insn = new InsnNode(InsnType.ONE_ARG, 1);
		insn.addArg(arg);
		return insn;
	}

	public void setResult(RegisterArg res) {
		if (res != null) {
			res.setParentInsn(this);
		}
		this.result = res;
	}

	public void addArg(InsnArg arg) {
		arg.setParentInsn(this);
		arguments.add(arg);
	}

	public InsnType getType() {
		return insnType;
	}

	public RegisterArg getResult() {
		return result;
	}

	public Iterable<InsnArg> getArguments() {
		return arguments;
	}

	public int getArgsCount() {
		return arguments.size();
	}

	public InsnArg getArg(int n) {
		return arguments.get(n);
	}

	public boolean containsArg(RegisterArg arg) {
		for (InsnArg a : arguments) {
			if (a == arg
					|| a.isRegister() && ((RegisterArg) a).getRegNum() == arg.getRegNum()) {
				return true;
			}
		}
		return false;
	}

	public void setArg(int n, InsnArg arg) {
		arg.setParentInsn(this);
		arguments.set(n, arg);
	}

	/**
	 * Replace instruction arg with another using recursive search.
	 * <br>
	 * <b>Caution:</b> this method don't change usage information for replaced argument.
	 */
	public boolean replaceArg(InsnArg from, InsnArg to) {
		int count = getArgsCount();
		for (int i = 0; i < count; i++) {
			InsnArg arg = arguments.get(i);
			if (arg == from) {
				setArg(i, to);
				return true;
			}
			if (arg.isInsnWrap() && ((InsnWrapArg) arg).getWrapInsn().replaceArg(from, to)) {
				return true;
			}
		}
		return false;
	}

	protected boolean removeArg(InsnArg arg) {
		int count = getArgsCount();
		for (int i = 0; i < count; i++) {
			if (arg == arguments.get(i)) {
				arguments.remove(i);
				if (arg instanceof RegisterArg) {
					RegisterArg reg = (RegisterArg) arg;
					reg.getSVar().removeUse(reg);
				}
				return true;
			}
		}
		return false;
	}

	protected void addReg(DecodedInstruction insn, int i, ArgType type) {
		addArg(InsnArg.reg(insn, i, type));
	}

	protected void addReg(int regNum, ArgType type) {
		addArg(InsnArg.reg(regNum, type));
	}

	protected void addLit(long literal, ArgType type) {
		addArg(InsnArg.lit(literal, type));
	}

	protected void addLit(DecodedInstruction insn, ArgType type) {
		addArg(InsnArg.lit(insn, type));
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public void getRegisterArgs(Collection<RegisterArg> collection) {
		for (InsnArg arg : this.getArguments()) {
			if (arg.isRegister()) {
				collection.add((RegisterArg) arg);
			} else if (arg.isInsnWrap()) {
				((InsnWrapArg) arg).getWrapInsn().getRegisterArgs(collection);
			}
		}
	}

	public boolean isConstInsn() {
		switch (getType()) {
			case CONST:
			case CONST_STR:
			case CONST_CLASS:
				return true;

			default:
				return false;
		}
	}

	public boolean canReorder() {
		switch (getType()) {
			case CONST:
			case CONST_STR:
			case CONST_CLASS:
			case CAST:
			case MOVE:
			case ARITH:
			case NEG:
			case CMP_L:
			case CMP_G:
			case CHECK_CAST:
			case INSTANCE_OF:
			case FILL_ARRAY:
			case FILLED_NEW_ARRAY:
			case NEW_ARRAY:
			case NEW_MULTIDIM_ARRAY:
			case STR_CONCAT:
				return true;

			default:
				return false;
		}
	}

	public boolean canReorderRecursive() {
		if (!canReorder()) {
			return false;
		}
		for (InsnArg arg : this.getArguments()) {
			if (arg.isInsnWrap()) {
				InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
				if (!wrapInsn.canReorderRecursive()) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return InsnUtils.formatOffset(offset) + ": "
				+ InsnUtils.insnTypeToString(insnType)
				+ (result == null ? "" : result + " = ")
				+ Utils.listToString(arguments);
	}

	/**
	 * Compare instruction only by identity.
	 */
	@Override
	public final int hashCode() {
		return super.hashCode();
	}

	/**
	 * Compare instruction only by identity.
	 */
	@Override
	public final boolean equals(Object obj) {
		return super.equals(obj);
	}

	/**
	 * 'Soft' equals, don't compare arguments, only instruction specific parameters.
	 */
	public boolean isSame(InsnNode other) {
		if (this == other) {
			return true;
		}
		if (insnType != other.insnType) {
			return false;
		}
		int size = arguments.size();
		if (size != other.arguments.size()) {
			return false;
		}
		// check wrapped instructions
		for (int i = 0; i < size; i++) {
			InsnArg arg = arguments.get(i);
			InsnArg otherArg = other.arguments.get(i);
			if (arg.isInsnWrap()) {
				if (!otherArg.isInsnWrap()) {
					return false;
				}
				InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
				InsnNode otherWrapInsn = ((InsnWrapArg) otherArg).getWrapInsn();
				if (!wrapInsn.isSame(otherWrapInsn)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 'Hard' equals, compare all arguments
	 */
	public boolean isDeepEquals(InsnNode other) {
		if (this == other) {
			return true;
		}
		return isSame(other)
				&& Objects.equals(arguments, other.arguments);
	}

	protected <T extends InsnNode> T copyCommonParams(T copy) {
		copy.setResult(result);
		if (copy.getArgsCount() == 0) {
			for (InsnArg arg : this.getArguments()) {
				if (arg.isInsnWrap()) {
					InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
					copy.addArg(InsnArg.wrapArg(wrapInsn.copy()));
				} else {
					copy.addArg(arg);
				}
			}
		}
		copy.copyAttributesFrom(this);
		copy.copyLines(this);
		copy.setOffset(this.getOffset());
		return copy;
	}

	/**
	 * Make copy of InsnNode object.
	 */
	public InsnNode copy() {
		if (this.getClass() == InsnNode.class) {
			return copyCommonParams(new InsnNode(insnType, getArgsCount()));
		}
		return INSN_CLONER.deepClone(this);
	}
}
