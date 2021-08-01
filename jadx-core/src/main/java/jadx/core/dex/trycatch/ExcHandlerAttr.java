package jadx.core.dex.trycatch;

import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.core.dex.attributes.AType;

public class ExcHandlerAttr implements IJadxAttribute {

	private final TryCatchBlock tryBlock;
	private final ExceptionHandler handler;

	public ExcHandlerAttr(TryCatchBlock block, ExceptionHandler handler) {
		this.tryBlock = block;
		this.handler = handler;
	}

	@Override
	public AType<ExcHandlerAttr> getAttrType() {
		return AType.EXC_HANDLER;
	}

	public TryCatchBlock getTryBlock() {
		return tryBlock;
	}

	public ExceptionHandler getHandler() {
		return handler;
	}

	@Override
	public String toString() {
		return "ExcHandler: " + (handler.isFinally()
				? " FINALLY"
				: handler.catchTypeStr() + ' ' + handler.getArg());
	}
}
