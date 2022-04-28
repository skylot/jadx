package jadx.api.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;

import jadx.api.ICodeInfo;

public class InMemoryCodeCache extends NoOpCodeCache {

	private final Map<String, ICodeInfo> storage = new ConcurrentHashMap<>();

	@Override
	public void add(String clsFullName, ICodeInfo codeInfo) {
		storage.put(clsFullName, codeInfo);
	}

	@Override
	public void remove(String clsFullName) {
		storage.remove(clsFullName);
	}

	@NotNull
	@Override
	public ICodeInfo get(String clsFullName) {
		ICodeInfo codeInfo = storage.get(clsFullName);
		if (codeInfo == null) {
			return ICodeInfo.EMPTY;
		}
		return codeInfo;
	}

	@Override
	public String toString() {
		return "InMemoryCodeCache: size=" + storage.size();
	}
}
