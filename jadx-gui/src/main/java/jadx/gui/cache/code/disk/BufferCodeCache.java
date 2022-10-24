package jadx.gui.cache.code.disk;

import java.io.IOException;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.api.ICodeCache;
import jadx.api.ICodeInfo;

public class BufferCodeCache implements ICodeCache {

	private static final int BUFFER_SIZE = 20;

	private final ICodeCache backCache;
	private final Map<String, ICodeInfo> cache = new ConcurrentHashMap<>();
	private final Deque<String> buffer = new ConcurrentLinkedDeque<>();

	public BufferCodeCache(ICodeCache backCache) {
		this.backCache = backCache;
	}

	private void addInternal(String clsFullName, ICodeInfo codeInfo) {
		cache.put(clsFullName, codeInfo);
		buffer.addLast(clsFullName);
		if (buffer.size() > BUFFER_SIZE) {
			String removedKey = buffer.removeFirst();
			cache.remove(removedKey);
		}
	}

	@Override
	public boolean contains(String clsFullName) {
		if (cache.containsKey(clsFullName)) {
			return true;
		}
		return backCache.contains(clsFullName);
	}

	@Override
	public void add(String clsFullName, ICodeInfo codeInfo) {
		addInternal(clsFullName, codeInfo);
		backCache.add(clsFullName, codeInfo);
	}

	@Override
	public @NotNull ICodeInfo get(String clsFullName) {
		ICodeInfo codeInfo = cache.get(clsFullName);
		if (codeInfo != null) {
			return codeInfo;
		}
		ICodeInfo backCodeInfo = backCache.get(clsFullName);
		if (backCodeInfo != ICodeInfo.EMPTY) {
			addInternal(clsFullName, backCodeInfo);
		}
		return backCodeInfo;
	}

	@Override
	public @Nullable String getCode(String clsFullName) {
		ICodeInfo codeInfo = cache.get(clsFullName);
		if (codeInfo != null) {
			return codeInfo.getCodeStr();
		}
		return backCache.getCode(clsFullName);
	}

	@Override
	public void remove(String clsFullName) {
		cache.remove(clsFullName);
		backCache.remove(clsFullName);
	}

	@Override
	public void close() throws IOException {
		cache.clear();
		buffer.clear();
		backCache.close();
	}
}
