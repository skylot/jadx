package jadx.core.plugins;

import jadx.api.JadxDecompiler;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.data.IJadxPlugins;
import jadx.api.plugins.data.JadxPluginRuntimeData;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class JadxPluginsData implements IJadxPlugins {

	private final JadxDecompiler decompiler;
	private final JadxPluginManager pluginManager;

	public JadxPluginsData(JadxDecompiler decompiler, JadxPluginManager pluginManager) {
		this.decompiler = decompiler;
		this.pluginManager = pluginManager;
	}

	@Override
	public JadxPluginRuntimeData getById(String pluginId) {
		return pluginManager.getResolvedPluginContexts()
				.stream()
				.filter(p -> p.getPluginId().equals(pluginId))
				.findFirst()
				.orElseThrow(() -> new JadxRuntimeException("Plugin with id '" + pluginId + "' not found"));
	}

	@Override
	public JadxPluginRuntimeData getProviding(String provideId) {
		return pluginManager.getResolvedPluginContexts()
				.stream()
				.filter(p -> p.getPluginInfo().getProvides().equals(provideId))
				.findFirst()
				.orElseThrow(() -> new JadxRuntimeException("Plugin providing '" + provideId + "' not found"));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <P extends JadxPlugin> P getInstance(Class<P> pluginCls) {
		return pluginManager.getResolvedPluginContexts()
				.stream()
				.filter(p -> p.getPluginInstance().getClass().equals(pluginCls))
				.map(p -> ((P) p.getPluginInstance()))
				.findFirst()
				.orElseThrow(() -> new JadxRuntimeException("Plugin class '" + pluginCls + "' not found"));
	}
}
