package jadx.core.dex.instructions;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.PrimitiveType;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

import com.android.dx.io.instructions.FillArrayDataPayloadDecodedInstruction;

public final class FillArrayNode extends InsnNode {

	private final Object data;
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
		this.elemType = elType;
	}

	public Object getData() {
		return data;
	}

	public ArgType getElementType() {
		return elemType;
	}

	public void mergeElementType(ArgType foundElemType) {
		ArgType r = ArgType.merge(elemType, foundElemType);
		if (r != null) {
			elemType = r;
		}
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
