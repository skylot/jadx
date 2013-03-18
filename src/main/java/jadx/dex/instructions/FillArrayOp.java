package jadx.dex.instructions;

import jadx.dex.instructions.args.ArgType;
import jadx.dex.instructions.args.InsnArg;
import jadx.dex.instructions.args.PrimitiveType;
import jadx.dex.nodes.InsnNode;
import jadx.dex.nodes.MethodNode;

import com.android.dx.io.instructions.FillArrayDataPayloadDecodedInstruction;

public class FillArrayOp extends InsnNode {

	private final Object data;

	public FillArrayOp(MethodNode method, int resReg, FillArrayDataPayloadDecodedInstruction payload) {
		super(method, InsnType.FILL_ARRAY, 0);

		this.data = payload.getData();

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
				throw new AssertionError();
		}
		setResult(InsnArg.reg(resReg, ArgType.array(elType)));
	}

	public Object getData() {
		return data;
	}
}
