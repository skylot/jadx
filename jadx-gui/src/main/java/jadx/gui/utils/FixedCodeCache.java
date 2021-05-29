package jadx.gui.utils;

import org.jetbrains.annotations.Nullable;

import jadx.api.ICodeCache;
import jadx.api.ICodeInfo;

/**
 * Code cache with fixed size of wrapper code cache ('remove' and 'add' methods will do nothing).
 */
public class FixedCodeCache implements ICodeCache {

	private final ICodeCache codeCache;

	public FixedCodeCache(ICodeCache codeCache) {
		this.codeCache = codeCache;
	}

	@Override
	public @Nullable ICodeInfo get(String clsFullName) {
		return this.codeCache.get(clsFullName);
	}

	@Override
	public void remove(String clsFullName) {
		// no op
	}

	@Override
	public void add(String clsFullName, ICodeInfo codeInfo) {
		// no op
	}
}
