package jadx.gui.utils.plugins;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.core.plugins.PluginRuntime;

public class CloseablePlugins {
	private final List<PluginRuntime> list;
	private final @Nullable Runnable closeable;

	public CloseablePlugins(List<PluginRuntime> list, @Nullable Runnable closeable) {
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

	public List<PluginRuntime> getList() {
		return list;
	}
}
