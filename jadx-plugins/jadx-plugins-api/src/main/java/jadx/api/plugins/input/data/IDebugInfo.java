package jadx.api.plugins.input.data;

import java.util.List;
import java.util.Map;

public interface IDebugInfo {

	/**
	 * Map instruction offset to source line number
	 */
	Map<Integer, Integer> getSourceLineMapping();

	List<ILocalVar> getLocalVars();
}
