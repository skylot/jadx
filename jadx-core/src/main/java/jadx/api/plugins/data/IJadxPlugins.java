package jadx.api.plugins.data;

import jadx.api.plugins.JadxPlugin;

public interface IJadxPlugins {

	JadxPluginRuntimeData getById(String pluginId);

	JadxPluginRuntimeData getProviding(String provideId);

	<P extends JadxPlugin> P getInstance(Class<P> pluginCls);
}
