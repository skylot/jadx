package jadx.api.plugins;

public interface JadxPlugin {
	JadxPluginInfo getPluginInfo();

	default void init(JadxPluginContext context) {
		// default to no-op
	}
}
