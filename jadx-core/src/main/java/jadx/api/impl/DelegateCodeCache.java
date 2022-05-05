package jadx.api.impl;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.api.ICodeCache;
import jadx.api.ICodeInfo;

public abstract class DelegateCodeCache implements ICodeCache {

	protected final ICodeCache backCache;

	public DelegateCodeCache(ICodeCache backCache) {
		this.backCache = backCache;
	}

	@Override
	public void add(String clsFullName, ICodeInfo codeInfo) {
		backCache.add(clsFullName, codeInfo);
	}

	@Override
	public void remove(String clsFullName) {
		backCache.remove(clsFullName);
	}

	@Override
	public @NotNull ICodeInfo get(String clsFullName) {
		return backCache.get(clsFullName);
	}

	@Override
	@Nullable
	public String getCode(String clsFullName) {
		return backCache.getCode(clsFullName);
	}

	@Override
	public boolean contains(String clsFullName) {
		return backCache.contains(clsFullName);
	}

	@Override
	public void close() throws IOException {
		backCache.close();
	}
}
