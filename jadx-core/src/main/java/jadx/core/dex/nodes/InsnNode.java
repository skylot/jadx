package jadx.core.dex.nodes;

import jadx.core.dex.attributes.LineAttrNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.android.dx.io.instructions.DecodedInstruction;

public class InsnNode extends LineAttrNode {

	protected final InsnType insnType;

	private RegisterArg result;
	private final List<InsnArg> arguments;
	protected int offset;
	protected int insnHashCode = super.hashCode();

	public InsnNode(InsnType type, int argsCount) {
		this.insnType = type;
		this.offset = -1;

		if (argsCount == 0) {
			this.arguments = Collections.emptyList();
		} else {
			this.arguments = new ArrayList<InsnArg>(argsCount);
		}
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
			if (a == arg || (a.isRegister() && ((RegisterArg) a).getRegNum() == arg.getRegNum())) {
				return true;
			}
		}
		return false;
	}

	public void setArg(int n, InsnArg arg) {
		arg.setParentInsn(this);
		arguments.set(n, arg);
	}

	public boolean replaceArg(InsnArg from, InsnArg to) {
		int count = getArgsCount();
		for (int i = 0; i < count; i++) {
			InsnArg arg = arguments.get(i);
			if (arg == from) {
				// TODO correct remove from use list
				setArg(i, to);
				return true;
			}
			if (arg.isInsnWrap() && ((InsnWrapArg) arg).getWrapInsn().replaceArg(from, to)) {
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

	public void getRegisterArgs(List<RegisterArg> list) {
		for (InsnArg arg : this.getArguments()) {
			if (arg.isRegister()) {
				list.add((RegisterArg) arg);
			} else if (arg.isInsnWrap()) {
				((InsnWrapArg) arg).getWrapInsn().getRegisterArgs(list);
			}
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
			case MOVE_EXCEPTION:
				return true;

			default:
				return false;
		}
	}

	@Override
	public String toString() {
		return InsnUtils.formatOffset(offset) + ": "
				+ InsnUtils.insnTypeToString(insnType)
				+ (result == null ? "" : result + " = ")
				+ Utils.listToString(arguments);
	}

	public void setInsnHashCode(int insnHashCode) {
		this.insnHashCode = insnHashCode;
	}

	@Override
	public int hashCode() {
		return insnHashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (hashCode() != obj.hashCode()) {
			return false;
		}
		if (!(obj instanceof InsnNode)) {
			return false;
		}
		InsnNode other = (InsnNode) obj;
		if (insnType != other.insnType) {
			return false;
		}
		if (arguments.size() != other.arguments.size()) {
			return false;
		}

		// TODO !!! finish equals
		return true;
	}

}
