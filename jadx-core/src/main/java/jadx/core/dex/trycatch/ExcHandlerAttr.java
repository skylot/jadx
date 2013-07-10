package jadx.core.dex.trycatch;

import jadx.core.dex.attributes.AttributeType;
import jadx.core.dex.attributes.IAttribute;

public class ExcHandlerAttr implements IAttribute {

	private final TryCatchBlock tryBlock;
	private final ExceptionHandler handler;

	public ExcHandlerAttr(TryCatchBlock block, ExceptionHandler handler) {
		this.tryBlock = block;
		this.handler = handler;
	}

	@Override
	public AttributeType getType() {
		return AttributeType.EXC_HANDLER;
	}

	public TryCatchBlock getTryBlock() {
		return tryBlock;
	}

	public ExceptionHandler getHandler() {
		return handler;
	}

	@Override
	public String toString() {
		return "ExcHandler: "
				+ (handler.isCatchAll() ? "all" : handler.getCatchType())
				+ " " + handler.getArg();
	}
}
