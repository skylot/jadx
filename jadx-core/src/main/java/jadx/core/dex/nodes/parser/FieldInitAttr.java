package jadx.core.dex.nodes.parser;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttribute;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;

public class FieldInitAttr implements IAttribute {

	public static final FieldInitAttr NULL_VALUE = constValue(null);

	public enum InitType {
		CONST,
		INSN

	}

	private final Object value;
	private final InitType valueType;
	private final MethodNode insnMth;

	private FieldInitAttr(InitType valueType, Object value, MethodNode insnMth) {
		this.value = value;
		this.valueType = valueType;
		this.insnMth = insnMth;
	}

	public static FieldInitAttr constValue(Object value) {
		return new FieldInitAttr(InitType.CONST, value, null);
	}

	public static FieldInitAttr insnValue(MethodNode mth, InsnNode insn) {
		return new FieldInitAttr(InitType.INSN, insn, mth);
	}

	public Object getValue() {
		return value;
	}

	public InsnNode getInsn() {
		return (InsnNode) value;
	}

	public InitType getValueType() {
		return valueType;
	}

	public MethodNode getInsnMth() {
		return insnMth;
	}

	@Override
	public AType<FieldInitAttr> getType() {
		return AType.FIELD_INIT;
	}

	@Override
	public String toString() {
		return "V=" + value;
	}
}
