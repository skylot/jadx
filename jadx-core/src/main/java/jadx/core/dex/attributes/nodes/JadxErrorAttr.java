package jadx.core.dex.attributes.nodes;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttribute;
import jadx.core.utils.Utils;

public class JadxErrorAttr implements IAttribute {

	private final Throwable cause;

	public JadxErrorAttr(Throwable cause) {
		this.cause = cause;
	}

	public Throwable getCause() {
		return cause;
	}

	@Override
	public AType<JadxErrorAttr> getType() {
		return AType.JADX_ERROR;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("JadxError: ");
		if (cause == null) {
			str.append("null");
		} else {
			str.append(cause.getClass());
			str.append(":");
			str.append(cause.getMessage());
			str.append("\n");
			str.append(Utils.getStackTrace(cause));
		}
		return str.toString();
	}

}
