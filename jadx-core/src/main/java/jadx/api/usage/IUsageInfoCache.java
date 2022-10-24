package jadx.api.usage;

import java.io.Closeable;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.nodes.RootNode;

public interface IUsageInfoCache extends Closeable {

	@Nullable
	IUsageInfoData get(RootNode root);

	void set(RootNode root, IUsageInfoData data);
}
