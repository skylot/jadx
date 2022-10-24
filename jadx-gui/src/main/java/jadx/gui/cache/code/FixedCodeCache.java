package jadx.gui.cache.code;

import jadx.api.ICodeCache;
import jadx.api.ICodeInfo;
import jadx.api.impl.DelegateCodeCache;

/**
 * Code cache with fixed size of wrapped code cache ('remove' and 'add' methods will do nothing).
 */
public class FixedCodeCache extends DelegateCodeCache {

	public FixedCodeCache(ICodeCache codeCache) {
		super(codeCache);
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
