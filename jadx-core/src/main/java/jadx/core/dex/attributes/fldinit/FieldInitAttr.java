package jadx.core.dex.attributes.fldinit;

import java.util.Objects;

import jadx.api.plugins.input.data.annotations.EncodedType;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttribute;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

public abstract class FieldInitAttr implements IAttribute {

	public static FieldInitAttr constValue(EncodedValue value) {
		if (Objects.equals(value, EncodedValue.NULL)) {
			return FieldInitConstAttr.NULL_VALUE;
		}
		return new FieldInitConstAttr(value);
	}

	public static FieldInitAttr hexConstValue(EncodedType type, Object value) {
		switch (type) {
			case ENCODED_BYTE:
			case ENCODED_SHORT:
			case ENCODED_CHAR:
			case ENCODED_INT:
			case ENCODED_LONG:
			case ENCODED_FLOAT:
			case ENCODED_DOUBLE:
				return new FieldInitHexConstAttr(value);
			default:
				throw new JadxRuntimeException("Unexpected encode type: " + type);
		}
	}

	public static FieldInitAttr insnValue(MethodNode mth, InsnNode insn) {
		return new FieldInitInsnAttr(mth, insn);
	}

	public boolean isConst() {
		return false;
	}

	public boolean isInsn() {
		return false;
	}

	public EncodedValue getEncodedValue() {
		throw new JadxRuntimeException("Wrong init type");
	}

	public InsnNode getInsn() {
		throw new JadxRuntimeException("Wrong init type");
	}

	public MethodNode getInsnMth() {
		throw new JadxRuntimeException("Wrong init type");
	}

	@Override
	public AType<FieldInitAttr> getType() {
		return AType.FIELD_INIT;
	}
}
