package jadx.api.plugins;

public interface JadxPlugin {

	JadxPluginInfo getPluginInfo();

	void init(JadxPluginContext context);
}
