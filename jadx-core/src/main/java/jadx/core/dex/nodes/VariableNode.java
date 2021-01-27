package jadx.core.dex.nodes;

import jadx.core.dex.attributes.nodes.LineAttrNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class VariableNode extends LineAttrNode {
	public enum VarKind {
		// note: better not change the order of these fields,
		// they are also used for variable renaming
		VAR, ARG, CATCH_ARG
	}

	public static final String VAR_SEPARATOR = "--->>"; // do not contain '='
	private VarKind varKind = VarKind.VAR;
	private ArgType type;
	private String name;
	private int index;
	MethodNode mth;

	public VariableNode(MethodNode mth, String name, ArgType type, VarKind varKind, int index) {
		this.mth = mth;
		this.name = name;
		this.type = type;
		this.index = index;
		this.varKind = varKind;
	}

	public MethodNode getMethodNode() {
		return mth;
	}

	public ClassNode getClassNode() {
		return mth.getParentClass();
	}

	public VarKind getVarKind() {
		return this.varKind;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public ArgType getType() {
		return type;
	}

	public int getIndex() {
		return index;
	}

	public String getRenameKey() {
		return mth.getMethodInfo().getRawFullId() + VAR_SEPARATOR + makeVarIndex(index, varKind);
	}

	public String makeVarIndex() {
		return makeVarIndex(index, varKind);
	}

	public static String makeVarIndex(int index, VarKind kind) {
		return kindToStr(kind) + "@" + kind.ordinal() + "_" + index;
	}

	private static String kindToStr(VarKind varKind) {
		String kind;
		switch (varKind) {
			case VAR:
				kind = "var";
				break;
			case ARG:
				kind = "param";
				break;
			case CATCH_ARG:
				kind = "catch";
				break;
			default:
				throw new JadxRuntimeException("Unexpected variable type " + varKind);
		}
		return kind;
	}

	@Override
	public int hashCode() {
		return mth.hashCode() + 31 * getDefPosition() + 31 * makeVarIndex().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}
}
