package jadx.api.plugins.input.data;

import java.util.List;

public interface IMethodRef {

	int getUniqId();

	/**
	 * Lazy loading for method info, until load() is called only getUniqId() can be used
	 */
	void load();

	String getParentClassType();

	String getName();

	String getReturnType();

	List<String> getArgTypes();
}
