package jadx.api.usage.impl;

import java.io.IOException;

import org.jetbrains.annotations.Nullable;

import jadx.api.usage.IUsageInfoCache;
import jadx.api.usage.IUsageInfoData;
import jadx.core.dex.nodes.RootNode;

public class EmptyUsageInfoCache implements IUsageInfoCache {
	@Override
	public @Nullable IUsageInfoData get(RootNode root) {
		return null;
	}

	@Override
	public void set(RootNode root, IUsageInfoData data) {
	}

	@Override
	public void close() throws IOException {
	}
}
