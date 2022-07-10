package jadx.api.impl;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;
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
	public @Nullable String getCode(String clsFullName) {
		ICodeInfo codeInfo = storage.get(clsFullName);
		if (codeInfo == null) {
			return null;
		}
		return codeInfo.getCodeStr();
	}

	@Override
	public boolean contains(String clsFullName) {
		return storage.containsKey(clsFullName);
	}

	@Override
	public void close() throws IOException {
		storage.clear();
	}

	@Override
	public String toString() {
		return "InMemoryCodeCache: size=" + storage.size();
	}
}
