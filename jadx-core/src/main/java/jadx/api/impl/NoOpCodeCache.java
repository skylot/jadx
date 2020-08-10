package jadx.api.impl;

import org.jetbrains.annotations.Nullable;

import jadx.api.ICodeCache;
import jadx.api.ICodeInfo;

public class NoOpCodeCache implements ICodeCache {

	@Override
	public void add(String clsFullName, ICodeInfo codeInfo) {
		// do nothing
	}

	@Override
	public void remove(String clsFullName) {
		// do nothing
	}

	@Override
	public @Nullable ICodeInfo get(String clsFullName) {
		return null;
	}

	@Override
	public String toString() {
		return "NoOpCodeCache";
	}
}
