package jadx.core.dex.instructions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.android.dx.io.instructions.FillArrayDataPayloadDecodedInstruction;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.PrimitiveType;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

public final class FillArrayNode extends InsnNode {

	private static final ArgType ONE_BYTE_TYPE = ArgType.unknown(PrimitiveType.BOOLEAN, PrimitiveType.BYTE);
	private static final ArgType TWO_BYTES_TYPE = ArgType.unknown(PrimitiveType.SHORT, PrimitiveType.CHAR);
	private static final ArgType FOUR_BYTES_TYPE = ArgType.unknown(PrimitiveType.INT, PrimitiveType.FLOAT);
	private static final ArgType EIGHT_BYTES_TYPE = ArgType.unknown(PrimitiveType.LONG, PrimitiveType.DOUBLE);

	private final Object data;
	private final int size;
	private ArgType elemType;

	public FillArrayNode(int resReg, FillArrayDataPayloadDecodedInstruction payload) {
		this(payload.getData(), payload.getSize(), getElementType(payload.getElementWidthUnit()));
		addArg(InsnArg.reg(resReg, ArgType.array(elemType)));
	}

	private FillArrayNode(Object data, int size, ArgType elemType) {
		super(InsnType.FILL_ARRAY, 1);
		this.data = data;
		this.size = size;
		this.elemType = elemType;
	}

	private static ArgType getElementType(short elementWidthUnit) {
		switch (elementWidthUnit) {
			case 1:
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
		if (array instanceof int[]) {
			for (int b : (int[]) array) {
				list.add(InsnArg.lit(b, type));
			}
		} else if (array instanceof byte[]) {
			for (byte b : (byte[]) array) {
				list.add(InsnArg.lit(b, type));
			}
		} else if (array instanceof short[]) {
			for (short b : (short[]) array) {
				list.add(InsnArg.lit(b, type));
			}
		} else if (array instanceof long[]) {
			for (long b : (long[]) array) {
				list.add(InsnArg.lit(b, type));
			}
		} else {
			throw new JadxRuntimeException("Unknown type: " + data.getClass() + ", expected: " + type);
		}
		return list;
	}

	@Override
	public boolean isSame(InsnNode obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof FillArrayNode) || !super.isSame(obj)) {
			return false;
		}
		FillArrayNode other = (FillArrayNode) obj;
		return elemType.equals(other.elemType) && data == other.data;
	}

	@Override
	public InsnNode copy() {
		return copyCommonParams(new FillArrayNode(data, size, elemType));
	}

	public String dataToString() {
		if (data instanceof int[]) {
			return Arrays.toString((int[]) data);
		}
		if (data instanceof short[]) {
			return Arrays.toString((short[]) data);
		}
		if (data instanceof byte[]) {
			return Arrays.toString((byte[]) data);
		}
		if (data instanceof long[]) {
			return Arrays.toString((long[]) data);
		}
		return "?";
	}

	@Override
	public String toString() {
		return super.toString() + ", data: " + dataToString();
	}
}
