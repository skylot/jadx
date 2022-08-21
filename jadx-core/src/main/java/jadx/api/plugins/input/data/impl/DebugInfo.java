package jadx.api.plugins.input.data.impl;

import java.util.List;
import java.util.Map;

import jadx.api.plugins.input.data.IDebugInfo;
import jadx.api.plugins.input.data.ILocalVar;

public class DebugInfo implements IDebugInfo {

	private final Map<Integer, Integer> sourceLineMap;
	private final List<ILocalVar> localVars;

	public DebugInfo(Map<Integer, Integer> sourceLineMap, List<ILocalVar> localVars) {
		this.sourceLineMap = sourceLineMap;
		this.localVars = localVars;
	}

	@Override
	public Map<Integer, Integer> getSourceLineMapping() {
		return sourceLineMap;
	}

	@Override
	public List<ILocalVar> getLocalVars() {
		return localVars;
	}

	@Override
	public String toString() {
		return "DebugInfo{lines=" + sourceLineMap + ", localVars=" + localVars + '}';
	}
}
