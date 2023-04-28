package jadx.core.dex.nodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import jadx.api.ICodeWriter;
import jadx.api.plugins.input.insns.InsnData;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.LineAttrNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.utils.InsnRemover;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class InsnNode extends LineAttrNode {
	protected final InsnType insnType;

	private RegisterArg result;
	private final List<InsnArg> arguments;
	protected int offset;

	public InsnNode(InsnType type, int argsCount) {
		this(type, argsCount == 0 ? Collections.emptyList() : new ArrayList<>(argsCount));
	}

	public InsnNode(InsnType type, List<InsnArg> args) {
		this.insnType = type;
		this.arguments = args;
		this.offset = -1;
		for (InsnArg arg : args) {
			attachArg(arg);
		}
	}

	public static InsnNode wrapArg(InsnArg arg) {
		InsnNode insn = new InsnNode(InsnType.ONE_ARG, 1);
		insn.addArg(arg);
		return insn;
	}

	public void setResult(@Nullable RegisterArg res) {
		this.result = res;
		if (res != null) {
			res.setParentInsn(this);
			SSAVar ssaVar = res.getSVar();
			if (ssaVar != null) {
				ssaVar.setAssign(res);
			}
		}
	}

	public void addArg(InsnArg arg) {
		arguments.add(arg);
		attachArg(arg);
	}

	public void setArg(int n, InsnArg arg) {
		arguments.set(n, arg);
		attachArg(arg);
	}

	protected void attachArg(InsnArg arg) {
		arg.setParentInsn(this);
		if (arg.isRegister()) {
			RegisterArg reg = (RegisterArg) arg;
			SSAVar ssaVar = reg.getSVar();
			if (ssaVar != null) {
				ssaVar.use(reg);
			}
		}
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

	public boolean containsArg(InsnArg arg) {
		if (getArgsCount() == 0) {
			return false;
		}
		for (InsnArg a : arguments) {
			if (a == arg) {
				return true;
			}
		}
		return false;
	}

	public boolean containsVar(RegisterArg arg) {
		if (getArgsCount() == 0) {
			return false;
		}
		return InsnUtils.containsVar(arguments, arg);
	}

	/**
	 * Replace instruction arg with another using recursive search.
	 */
	public boolean replaceArg(InsnArg from, InsnArg to) {
		int count = getArgsCount();
		for (int i = 0; i < count; i++) {
			InsnArg arg = arguments.get(i);
			if (arg == from) {
				InsnRemover.unbindArgUsage(null, arg);
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
		int index = getArgIndex(arg);
		if (index == -1) {
			return false;
		}
		removeArg(index);
		return true;
	}

	public InsnArg removeArg(int index) {
		InsnArg arg = arguments.get(index);
		arguments.remove(index);
		InsnRemover.unbindArgUsage(null, arg);
		return arg;
	}

	public int getArgIndex(InsnArg arg) {
		int count = getArgsCount();
		for (int i = 0; i < count; i++) {
			if (arg == arguments.get(i)) {
				return i;
			}
		}
		return -1;
	}

	protected void addReg(InsnData insn, int i, ArgType type) {
		addArg(InsnArg.reg(insn, i, type));
	}

	protected void addReg(int regNum, ArgType type) {
		addArg(InsnArg.reg(regNum, type));
	}

	protected void addLit(long literal, ArgType type) {
		addArg(InsnArg.lit(literal, type));
	}

	protected void addLit(InsnData insn, ArgType type) {
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

	public boolean canRemoveResult() {
		switch (getType()) {
			case INVOKE:
			case CONSTRUCTOR:
				return true;

			default:
				return false;
		}
	}

	public boolean canReorder() {
		if (contains(AFlag.DONT_GENERATE)) {
			if (getType() == InsnType.MONITOR_EXIT) {
				return false;
			}
			return true;
		}
		for (InsnArg arg : getArguments()) {
			if (arg.isInsnWrap()) {
				InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
				if (!wrapInsn.canReorder()) {
					return false;
				}
			}
		}

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
			case STR_CONCAT:
				return true;

			case SGET:
			case IGET:
				// TODO: allow to move final fields
				return false;

			default:
				return false;
		}
	}

	public boolean containsWrappedInsn() {
		for (InsnArg arg : this.getArguments()) {
			if (arg.isInsnWrap()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Visit this instruction and all inner (wrapped) instructions
	 */
	public void visitInsns(Consumer<InsnNode> visitor) {
		visitor.accept(this);
		for (InsnArg arg : this.getArguments()) {
			if (arg.isInsnWrap()) {
				((InsnWrapArg) arg).getWrapInsn().visitInsns(visitor);
			}
		}
	}

	/**
	 * Visit this instruction and all inner (wrapped) instructions
	 * To terminate visiting return non-null value
	 */
	@Nullable
	public <R> R visitInsns(Function<InsnNode, R> visitor) {
		R result = visitor.apply(this);
		if (result != null) {
			return result;
		}
		for (InsnArg arg : this.getArguments()) {
			if (arg.isInsnWrap()) {
				InsnNode innerInsn = ((InsnWrapArg) arg).getWrapInsn();
				R res = innerInsn.visitInsns(visitor);
				if (res != null) {
					return res;
				}
			}
		}
		return null;
	}

	/**
	 * Visit all args recursively (including inner instructions), but excluding wrapped args
	 */
	public void visitArgs(Consumer<InsnArg> visitor) {
		for (InsnArg arg : getArguments()) {
			if (arg.isInsnWrap()) {
				((InsnWrapArg) arg).getWrapInsn().visitArgs(visitor);
			} else {
				visitor.accept(arg);
			}
		}
	}

	/**
	 * Visit all args recursively (including inner instructions), but excluding wrapped args.
	 * To terminate visiting return non-null value
	 */
	@Nullable
	public <R> R visitArgs(Function<InsnArg, R> visitor) {
		for (InsnArg arg : getArguments()) {
			R result;
			if (arg.isInsnWrap()) {
				InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
				result = wrapInsn.visitArgs(visitor);
			} else {
				result = visitor.apply(arg);
			}
			if (result != null) {
				return result;
			}
		}
		return null;
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
				&& Objects.equals(result, other.result)
				&& Objects.equals(arguments, other.arguments);
	}

	protected final <T extends InsnNode> T copyCommonParams(T copy) {
		if (copy.getArgsCount() == 0) {
			for (InsnArg arg : this.getArguments()) {
				copy.addArg(arg.duplicate());
			}
		}
		copy.copyAttributesFrom(this);
		copy.copyLines(this);
		copy.setOffset(this.getOffset());
		return copy;
	}

	public void copyAttributesFrom(InsnNode attrNode) {
		super.copyAttributesFrom(attrNode);
		this.addSourceLineFrom(attrNode);
	}

	/**
	 * Make copy of InsnNode object.
	 * <br>
	 * NOTE: can't copy instruction with result argument
	 * (SSA variable can't be used in two different assigns).
	 * <br>
	 * Prefer use next methods:
	 * <ul>
	 * <li>{@link #copyWithoutResult()} to explicitly state that result not needed
	 * <li>{@link #copy(RegisterArg)} to provide new result arg
	 * <li>{@link #copyWithNewSsaVar(MethodNode)} to make new SSA variable for result arg
	 * </ul>
	 */
	public InsnNode copy() {
		if (this.getClass() != InsnNode.class) {
			throw new JadxRuntimeException("Copy method not implemented in insn class " + this.getClass().getSimpleName());
		}
		return copyCommonParams(new InsnNode(insnType, getArgsCount()));
	}

	/**
	 * See {@link #copy()}
	 */
	@SuppressWarnings("unchecked")
	public <T extends InsnNode> T copyWithoutResult() {
		return (T) copy();
	}

	public InsnNode copyWithoutSsa() {
		InsnNode copy = copyWithoutResult();
		if (result != null) {
			if (result.getSVar() == null) {
				copy.setResult(result.duplicate());
			} else {
				throw new JadxRuntimeException("Can't copy if SSA var is set");
			}
		}
		return copy;
	}

	/**
	 * See {@link #copy()}
	 */
	public InsnNode copy(RegisterArg newReturnArg) {
		InsnNode copy = copy();
		copy.setResult(newReturnArg);
		return copy;
	}

	/**
	 * See {@link #copy()}
	 */
	public InsnNode copyWithNewSsaVar(MethodNode mth) {
		RegisterArg result = getResult();
		if (result == null) {
			throw new JadxRuntimeException("Result in null");
		}
		int regNum = result.getRegNum();
		RegisterArg resDupArg = result.duplicate(regNum, null);
		mth.makeNewSVar(resDupArg);
		return copy(resDupArg);
	}

	/**
	 * Fix SSAVar info in register arguments.
	 * Must be used after altering instructions.
	 */
	public void rebindArgs() {
		RegisterArg resArg = getResult();
		if (resArg != null) {
			SSAVar ssaVar = resArg.getSVar();
			if (ssaVar == null) {
				throw new JadxRuntimeException("No SSA var for result arg: " + resArg + " from " + resArg.getParentInsn());
			}
			ssaVar.setAssign(resArg);
		}
		for (InsnArg arg : getArguments()) {
			if (arg instanceof RegisterArg) {
				RegisterArg reg = (RegisterArg) arg;
				SSAVar ssaVar = reg.getSVar();
				ssaVar.use(reg);
				ssaVar.updateUsedInPhiList();
			} else if (arg instanceof InsnWrapArg) {
				((InsnWrapArg) arg).getWrapInsn().rebindArgs();
			}
		}
	}

	public boolean canThrowException() {
		switch (getType()) {
			case RETURN:
			case IF:
			case GOTO:
			case MOVE:
			case MOVE_EXCEPTION:
			case NEG:
			case CONST:
			case CONST_STR:
			case CONST_CLASS:
			case CMP_L:
			case CMP_G:
			case NOP:
				return false;

			default:
				return true;
		}
	}

	public void inheritMetadata(InsnNode sourceInsn) {
		if (insnType == InsnType.RETURN) {
			this.copyLines(sourceInsn);
			if (this.contains(AFlag.SYNTHETIC)) {
				this.setOffset(sourceInsn.getOffset());
				this.rewriteAttributeFrom(sourceInsn, AType.CODE_COMMENTS);
			} else {
				this.copyAttributeFrom(sourceInsn, AType.CODE_COMMENTS);
			}
		} else {
			this.copyAttributeFrom(sourceInsn, AType.CODE_COMMENTS);
			this.addSourceLineFrom(sourceInsn);
		}
	}

	/**
	 * Compare instruction only by identity.
	 */
	@SuppressWarnings("EmptyMethod")
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
	 * Append arguments type, wrap line if too long
	 *
	 * @return true if args wrapped
	 */
	protected boolean appendArgs(StringBuilder sb) {
		if (arguments.isEmpty()) {
			return false;
		}
		String argsStr = Utils.listToString(arguments);
		if (argsStr.length() < 120) {
			sb.append(argsStr);
			return false;
		}
		// wrap args
		String separator = ICodeWriter.NL + "  ";
		sb.append(separator).append(Utils.listToString(arguments, separator));
		sb.append(ICodeWriter.NL);
		return true;
	}

	protected String attributesString() {
		StringBuilder sb = new StringBuilder();
		appendAttributes(sb);
		return sb.toString();
	}

	protected void appendAttributes(StringBuilder sb) {
		if (!isAttrStorageEmpty()) {
			sb.append(' ').append(getAttributesString());
		}
		if (getSourceLine() != 0) {
			sb.append(" (LINE:").append(getSourceLine()).append(')');
		}
	}

	protected String baseString() {
		StringBuilder sb = new StringBuilder();
		if (offset != -1) {
			sb.append(InsnUtils.formatOffset(offset)).append(": ");
		}
		sb.append(insnType).append(' ');
		if (result != null) {
			sb.append(result).append(" = ");
		}
		appendArgs(sb);
		return sb.toString();
	}

	@Override
	public String toString() {
		return baseString() + attributesString();
	}
}
