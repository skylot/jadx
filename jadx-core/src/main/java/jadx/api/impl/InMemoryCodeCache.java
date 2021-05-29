package jadx.api.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import jadx.api.ICodeCache;
import jadx.api.ICodeInfo;

public class InMemoryCodeCache implements ICodeCache {

	private final Map<String, ICodeInfo> storage = new ConcurrentHashMap<>();

	@Override
	public void add(String clsFullName, ICodeInfo codeInfo) {
		storage.put(clsFullName, codeInfo);
	}

	@Override
	public void remove(String clsFullName) {
		storage.remove(clsFullName);
	}

	@Override
	public @Nullable ICodeInfo get(String clsFullName) {
		return storage.get(clsFullName);
	}

	@Override
	public String toString() {
		return "InMemoryCodeCache: size=" + storage.size();
	}
}
