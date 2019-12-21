package jadx.api;

import java.util.Map;

import jadx.api.impl.SimpleCodeInfo;

public interface ICodeInfo {

	ICodeInfo EMPTY = new SimpleCodeInfo("");

	String getCodeStr();

	Map<Integer, Integer> getLineMapping();

	Map<CodePosition, Object> getAnnotations();
}
