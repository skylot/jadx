package jadx.api.usage.impl;

import org.jetbrains.annotations.Nullable;

import jadx.api.usage.IUsageInfoCache;
import jadx.api.usage.IUsageInfoData;
import jadx.core.dex.nodes.RootNode;

public class InMemoryUsageInfoCache implements IUsageInfoCache {

	private IUsageInfoData data;

	/**
	 * `data` field tied to root node instance, keep hash to reset cache on change
	 */
	private int rootNodeHash;

	@Override
	public @Nullable IUsageInfoData get(RootNode root) {
		return rootNodeHash == root.hashCode() ? data : null;
	}

	@Override
	public void set(RootNode root, IUsageInfoData data) {
		this.rootNodeHash = root.hashCode();
		this.data = data;
	}

	@Override
	public void close() {
		this.rootNodeHash = 0;
		this.data = null;
	}
}
