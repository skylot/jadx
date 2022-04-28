package jadx.api.impl;

import java.util.Collections;
import java.util.Map;

import jadx.api.CodePosition;
import jadx.api.ICodeInfo;
import jadx.api.metadata.ICodeMetadata;

public class SimpleCodeInfo implements ICodeInfo {

	private final String code;

	public SimpleCodeInfo(String code) {
		this.code = code;
	}

	@Override
	public String getCodeStr() {
		return code;
	}

	@Override
	public Map<Integer, Integer> getLineMapping() {
		return Collections.emptyMap();
	}

	@Override
	public Map<CodePosition, Object> getAnnotations() {
		return Collections.emptyMap();
	}

	@Override
	public ICodeMetadata getCodeMetadata() {
		return ICodeMetadata.EMPTY;
	}

	@Override
	public boolean hasMetadata() {
		return false;
	}

	@Override
	public String toString() {
		return code;
	}
}
