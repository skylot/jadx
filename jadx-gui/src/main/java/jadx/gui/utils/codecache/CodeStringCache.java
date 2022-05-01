package jadx.gui.utils.codecache;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import jadx.api.ICodeCache;
import jadx.api.ICodeInfo;
import jadx.api.impl.DelegateCodeCache;

/**
 * Keep code strings for faster search
 */
public class CodeStringCache extends DelegateCodeCache {

	private final Map<String, String> codeCache = new ConcurrentHashMap<>();

	public CodeStringCache(ICodeCache backCache) {
		super(backCache);
	}

	@Override
	@Nullable
	public String getCode(String clsFullName) {
		String code = codeCache.get(clsFullName);
		if (code != null) {
			return code;
		}
		String backCode = backCache.getCode(clsFullName);
		if (backCode != null) {
			codeCache.put(clsFullName, backCode);
		}
		return backCode;
	}

	@Override
	public void add(String clsFullName, ICodeInfo codeInfo) {
		codeCache.put(clsFullName, codeInfo.getCodeStr());
		backCache.add(clsFullName, codeInfo);
	}

	@Override
	public void remove(String clsFullName) {
		codeCache.remove(clsFullName);
		backCache.remove(clsFullName);
	}

	@Override
	public void close() throws IOException {
		codeCache.clear();
		backCache.close();
	}
}
