package jadx.core.dex.instructions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jadx.api.plugins.input.insns.custom.IArrayPayload;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.PrimitiveType;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

public final class FillArrayData extends InsnNode {

	private static final ArgType ONE_BYTE_TYPE = ArgType.unknown(PrimitiveType.BYTE, PrimitiveType.BOOLEAN);
	private static final ArgType TWO_BYTES_TYPE = ArgType.unknown(PrimitiveType.SHORT, PrimitiveType.CHAR);
	private static final ArgType FOUR_BYTES_TYPE = ArgType.unknown(PrimitiveType.INT, PrimitiveType.FLOAT);
	private static final ArgType EIGHT_BYTES_TYPE = ArgType.unknown(PrimitiveType.LONG, PrimitiveType.DOUBLE);

	private final Object data;
	private final int size;
	private final int elemSize;
	private ArgType elemType;

	public FillArrayData(IArrayPayload payload) {
		this(payload.getData(), payload.getSize(), payload.getElementSize());
	}

	private FillArrayData(Object data, int size, int elemSize) {
		super(InsnType.FILL_ARRAY_DATA, 0);
		this.data = data;
		this.size = size;
		this.elemSize = elemSize;
		this.elemType = getElementType(elemSize);
	}

	private static ArgType getElementType(int elementWidthUnit) {
		switch (elementWidthUnit) {
			case 1:
			case 0:
				return ONE_BYTE_TYPE;
			case 2:
				return TWO_BYTES_TYPE;
			case 4:
				return FOUR_BYTES_TYPE;
			case 8:
				return EIGHT_BYTES_TYPE;
			default:
				throw new JadxRuntimeException("Unknown array element width: " + elementWidthUnit);
		}
	}

	public Object getData() {
		return data;
	}

	public int getSize() {
		return size;
	}

	public ArgType getElementType() {
		return elemType;
	}

	public List<LiteralArg> getLiteralArgs(ArgType type) {
		List<LiteralArg> list = new ArrayList<>(size);
		Object array = data;
		switch (elemSize) {
			case 1:
				for (byte b : (byte[]) array) {
					list.add(InsnArg.lit(b, type));
				}
				break;
			case 2:
				for (short b : (short[]) array) {
					list.add(InsnArg.lit(b, type));
				}
				break;
			case 4:
				for (int b : (int[]) array) {
					list.add(InsnArg.lit(b, type));
				}
				break;
			case 8:
				for (long b : (long[]) array) {
					list.add(InsnArg.lit(b, type));
				}
				break;
			default:
				throw new JadxRuntimeException("Unknown type: " + data.getClass() + ", expected: " + type);
		}
		return list;
	}

	@Override
	public boolean isSame(InsnNode obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof FillArrayData) || !super.isSame(obj)) {
			return false;
		}
		FillArrayData other = (FillArrayData) obj;
		return elemType.equals(other.elemType) && data == other.data;
	}

	@Override
	public InsnNode copy() {
		FillArrayData copy = new FillArrayData(data, size, elemSize);
		copy.elemType = this.elemType;
		return copyCommonParams(copy);
	}

	public String dataToString() {
		switch (elemSize) {
			case 1:
				return Arrays.toString((byte[]) data);
			case 2:
				return Arrays.toString((short[]) data);
			case 4:
				return Arrays.toString((int[]) data);
			case 8:
				return Arrays.toString((long[]) data);
			default:
				return "?";
		}
	}

	@Override
	public String toString() {
		return super.toString() + ", data: " + dataToString();
	}
}
