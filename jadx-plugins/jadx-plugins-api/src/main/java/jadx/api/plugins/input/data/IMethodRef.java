package jadx.api.plugins.input.data;

import jadx.api.plugins.input.insns.custom.ICustomPayload;

public interface IMethodRef extends IMethodProto, ICustomPayload {

	int getUniqId();

	/**
	 * Lazy loading for method info, until load() is called only getUniqId() can be used
	 */
	void load();

	String getParentClassType();

	String getName();
}
