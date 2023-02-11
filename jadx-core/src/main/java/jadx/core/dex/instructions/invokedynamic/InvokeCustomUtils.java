package jadx.core.dex.instructions.invokedynamic;

import jadx.api.plugins.input.data.MethodHandleType;
import jadx.core.dex.instructions.InvokeType;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class InvokeCustomUtils {

	public static InvokeType convertInvokeType(MethodHandleType type) {
		switch (type) {
			case INVOKE_STATIC:
				return InvokeType.STATIC;
			case INVOKE_INSTANCE:
				return InvokeType.VIRTUAL;
			case INVOKE_DIRECT:
			case INVOKE_CONSTRUCTOR:
				return InvokeType.DIRECT;
			case INVOKE_INTERFACE:
				return InvokeType.INTERFACE;

			default:
				throw new JadxRuntimeException("Unsupported method handle type: " + type);
		}
	}
}
