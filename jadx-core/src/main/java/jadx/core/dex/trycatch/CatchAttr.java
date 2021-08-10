package jadx.core.dex.trycatch;

import java.util.List;

import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.core.dex.attributes.AType;
import jadx.core.utils.Utils;

public class CatchAttr implements IJadxAttribute {

	private final List<ExceptionHandler> handlers;

	public CatchAttr(List<ExceptionHandler> handlers) {
		this.handlers = handlers;
	}

	public List<ExceptionHandler> getHandlers() {
		return handlers;
	}

	@Override
	public AType<CatchAttr> getAttrType() {
		return AType.EXC_CATCH;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CatchAttr)) {
			return false;
		}
		CatchAttr catchAttr = (CatchAttr) o;
		return getHandlers().equals(catchAttr.getHandlers());
	}

	@Override
	public int hashCode() {
		return getHandlers().hashCode();
	}

	@Override
	public String toString() {
		return "Catch: " + Utils.listToString(getHandlers());
	}
}
