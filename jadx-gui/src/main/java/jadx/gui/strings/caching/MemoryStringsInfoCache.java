package jadx.gui.strings.caching;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.IClassData;
import jadx.core.dex.nodes.ClassNode;
import jadx.gui.strings.SingleStringResult;

public class MemoryStringsInfoCache implements IStringsInfoCache {

	private final Map<Integer, SoftReference<List<SingleStringResult>>> memoryCache;

	public MemoryStringsInfoCache() {
		this.memoryCache = new ConcurrentHashMap<>();
	}

	@Override
	public void close() throws IOException {
		this.memoryCache.clear();
	}

	@Override
	public void addResults(final ClassNode clsNode, final List<SingleStringResult> stringResult) {
		final IClassData clsData = clsNode.getClsData();
		if (clsData == null) {
			return;
		}

		final int id = getUniqueClassId(clsData);
		final SoftReference<List<SingleStringResult>> reference = new SoftReference<>(stringResult);
		this.memoryCache.put(id, reference);
	}

	@Override
	public void remove(final ClassNode clsNode) {
		final IClassData clsData = clsNode.getClsData();
		if (clsData == null) {
			return;
		}

		final int id = getUniqueClassId(clsData);
		this.memoryCache.remove(id);
	}

	@Override
	public @Nullable List<SingleStringResult> getStrings(final ClassNode clsNode) {
		final IClassData clsData = clsNode.getClsData();
		if (clsData == null) {
			return null;
		}

		final int id = getUniqueClassId(clsData);
		final SoftReference<List<SingleStringResult>> reference = this.memoryCache.get(id);
		if (reference == null) {
			// No cache exists for this class
			return null;
		}

		final List<SingleStringResult> results = reference.get();
		if (results == null) {
			// Results have been invalidated
			this.memoryCache.remove(id);
		}
		return results;
	}

	@Override
	public boolean contains(final ClassNode clsNode) {
		final IClassData clsData = clsNode.getClsData();
		if (clsData == null) {
			return false;
		}

		final int id = getUniqueClassId(clsData);
		final SoftReference<List<SingleStringResult>> reference = this.memoryCache.get(id);
		if (reference == null) {
			return false;
		}

		final boolean hasBeenInvalidated = reference.get() == null;
		if (hasBeenInvalidated) {
			this.memoryCache.remove(id);
		}
		return !hasBeenInvalidated;
	}

	@Override
	public @NotNull Set<Integer> getContainedClasses() {
		cleanInvalidEntries();
		return this.memoryCache.keySet();
	}

	private void cleanInvalidEntries() {
		final Set<Integer> uniqueClassIds = this.memoryCache.keySet();
		for (final Integer uniqueClassId : uniqueClassIds) {
			final SoftReference<List<SingleStringResult>> resultsForClassRef = this.memoryCache.get(uniqueClassId);
			final List<SingleStringResult> resultsForClass = resultsForClassRef.get();
			if (resultsForClass == null) {
				this.memoryCache.remove(uniqueClassId);
			}
		}
	}

	private int getUniqueClassId(final IClassData data) {
		return Objects.hash(data.getInputFileName(), data.getInputFileOffset());
	}
}
