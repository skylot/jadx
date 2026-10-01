package jadx.core.plugins;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.options.JadxPluginOptions;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class PluginRuntime implements Comparable<PluginRuntime> {
	private static final Logger LOG = LoggerFactory.getLogger(PluginRuntime.class);

	private final JadxPlugin plugin;
	private final JadxPluginInfo pluginInfo;
	private final ClassLoader pluginClassLoader;
	private final JadxArgs jadxArgs;

	private @Nullable AppContext appContext;
	private @Nullable PluginContext pluginContext;

	/**
	 * Plugin options stored here to allow 'global' options without project (decompiler).
	 * TODO: improve API to distinguish global and project options for jadx-gui
	 */
	private @Nullable JadxPluginOptions options;

	public PluginRuntime(JadxPlugin plugin, JadxArgs jadxArgs) {
		this.plugin = plugin;
		this.pluginInfo = plugin.getPluginInfo();
		this.pluginClassLoader = plugin.getClass().getClassLoader();
		this.jadxArgs = jadxArgs;
	}

	public void init(PluginContext pluginContext) {
		this.pluginContext = Objects.requireNonNull(pluginContext);
		classLoaderWrap(() -> {
			try {
				plugin.init(pluginContext);
			} catch (Throwable e) {
				LOG.error("Plugin init failed", e);
				this.pluginContext = null;
			}
		});
	}

	public void unload() {
		if (pluginContext != null) {
			try {
				classLoaderWrap(plugin::unload);
			} finally {
				pluginContext = null;
			}
		}
	}

	public void classLoaderWrap(Runnable task) {
		classLoaderWrap(pluginClassLoader, task);
	}

	public static void classLoaderWrap(ClassLoader pluginClassLoader, Runnable task) {
		Thread thread = Thread.currentThread();
		ClassLoader prevClassLoader = thread.getContextClassLoader();
		thread.setContextClassLoader(pluginClassLoader);
		try {
			task.run();
		} finally {
			thread.setContextClassLoader(prevClassLoader);
		}
	}

	public void registerOptions(@Nullable JadxPluginOptions options) {
		if (options == null) {
			return;
		}
		this.options = options;
		try {
			options.setOptions(jadxArgs.getPluginOptions());
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to apply options for plugin: " + getPluginId(), e);
		}
	}

	public JadxPlugin getPluginInstance() {
		return plugin;
	}

	public JadxPluginInfo getPluginInfo() {
		return pluginInfo;
	}

	public String getPluginId() {
		return pluginInfo.getPluginId();
	}

	public @Nullable AppContext getAppContext() {
		return appContext;
	}

	public void setAppContext(@Nullable AppContext appContext) {
		this.appContext = appContext;
	}

	public @Nullable PluginContext getPluginContext() {
		return pluginContext;
	}

	public boolean isInitialized() {
		return pluginContext != null;
	}

	public @Nullable JadxPluginOptions getOptions() {
		return options;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof PluginRuntime)) {
			return false;
		}
		return this.getPluginId().equals(((PluginRuntime) other).getPluginId());
	}

	@Override
	public int hashCode() {
		return getPluginId().hashCode();
	}

	@Override
	public int compareTo(PluginRuntime other) {
		return this.getPluginId().compareTo(other.getPluginId());
	}

	@Override
	public String toString() {
		return getPluginId();
	}
}
