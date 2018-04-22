package jadx.core.dex.instructions;

import java.util.ArrayList;
import java.util.List;

import com.android.dx.io.instructions.FillArrayDataPayloadDecodedInstruction;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.PrimitiveType;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

public final class FillArrayNode extends InsnNode {

	private final Object data;
	private final int size;
	private ArgType elemType;

	public FillArrayNode(int resReg, FillArrayDataPayloadDecodedInstruction payload) {
		super(InsnType.FILL_ARRAY, 0);
		ArgType elType;
		switch (payload.getElementWidthUnit()) {
			case 1:
				elType = ArgType.unknown(PrimitiveType.BOOLEAN, PrimitiveType.BYTE);
				break;
			case 2:
				elType = ArgType.unknown(PrimitiveType.SHORT, PrimitiveType.CHAR);
				break;
			case 4:
				elType = ArgType.unknown(PrimitiveType.INT, PrimitiveType.FLOAT);
				break;
			case 8:
				elType = ArgType.unknown(PrimitiveType.LONG, PrimitiveType.DOUBLE);
				break;

			default:
				throw new JadxRuntimeException("Unknown array element width: " + payload.getElementWidthUnit());
		}
		setResult(InsnArg.reg(resReg, ArgType.array(elType)));

		this.data = payload.getData();
		this.size = payload.getSize();
		this.elemType = elType;
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

	public void mergeElementType(DexNode dex, ArgType foundElemType) {
		ArgType r = ArgType.merge(dex, elemType, foundElemType);
		if (r != null) {
			elemType = r;
		}
	}

	public List<LiteralArg> getLiteralArgs() {
		List<LiteralArg> list = new ArrayList<>(size);
		Object array = data;
		if (array instanceof int[]) {
			for (int b : (int[]) array) {
				list.add(InsnArg.lit(b, elemType));
			}
		} else if (array instanceof byte[]) {
			for (byte b : (byte[]) array) {
				list.add(InsnArg.lit(b, elemType));
			}
		} else if (array instanceof short[]) {
			for (short b : (short[]) array) {
				list.add(InsnArg.lit(b, elemType));
			}
		} else if (array instanceof long[]) {
			for (long b : (long[]) array) {
				list.add(InsnArg.lit(b, elemType));
			}
		} else {
			throw new JadxRuntimeException("Unknown type: " + data.getClass() + ", expected: " + elemType);
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
}
