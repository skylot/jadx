package jadx.gui.utils.plugins;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.core.plugins.PluginContext;

public class CloseablePlugins {
	private final List<PluginContext> list;
	private final @Nullable Runnable closeable;

	public CloseablePlugins(List<PluginContext> list, @Nullable Runnable closeable) {
		this.list = list;
		this.closeable = closeable;
	}

	public void close() {
		if (closeable != null) {
			closeable.run();
		}
	}

	public @Nullable Runnable getCloseable() {
		return closeable;
	}

	public List<PluginContext> getList() {
		return list;
	}
}
