package jadx.api;

import java.util.Map;

public interface ICodeInfo {
	String getCodeStr();

	Map<Integer, Integer> getLineMapping();

	Map<CodePosition, Object> getAnnotations();
}
