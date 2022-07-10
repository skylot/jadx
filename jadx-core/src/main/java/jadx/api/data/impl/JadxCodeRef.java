package jadx.api.data.impl;

import jadx.api.JavaVariable;
import jadx.api.data.CodeRefType;
import jadx.api.data.IJavaCodeRef;
import jadx.api.metadata.annotations.VarNode;

public class JadxCodeRef implements IJavaCodeRef {

	public static JadxCodeRef forInsn(int offset) {
		return new JadxCodeRef(CodeRefType.INSN, offset);
	}

	public static JadxCodeRef forMthArg(int argIndex) {
		return new JadxCodeRef(CodeRefType.MTH_ARG, argIndex);
	}

	public static JadxCodeRef forVar(int regNum, int ssaVersion) {
		return new JadxCodeRef(CodeRefType.VAR, regNum << 16 | ssaVersion);
	}

	public static JadxCodeRef forVar(JavaVariable javaVariable) {
		return forVar(javaVariable.getReg(), javaVariable.getSsa());
	}

	public static JadxCodeRef forVar(VarNode varNode) {
		return forVar(varNode.getReg(), varNode.getSsa());
	}

	public static JadxCodeRef forCatch(int handlerOffset) {
		return new JadxCodeRef(CodeRefType.CATCH, handlerOffset);
	}

	private CodeRefType attachType;
	private int index;

	public JadxCodeRef(CodeRefType attachType, int index) {
		this.attachType = attachType;
		this.index = index;
	}

	public JadxCodeRef() {
		// used for json serialization
	}

	public CodeRefType getAttachType() {
		return attachType;
	}

	public void setAttachType(CodeRefType attachType) {
		this.attachType = attachType;
	}

	@Override
	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof JadxCodeRef)) {
			return false;
		}
		JadxCodeRef other = (JadxCodeRef) o;
		return getIndex() == other.getIndex()
				&& getAttachType() == other.getAttachType();
	}

	@Override
	public int hashCode() {
		return 31 * getAttachType().hashCode() + getIndex();
	}

	@Override
	public String toString() {
		return "JadxCodeRef{"
				+ "attachType=" + attachType
				+ ", index=" + index
				+ '}';
	}
}
