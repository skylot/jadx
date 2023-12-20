package jadx.api.plugins;

import jadx.api.plugins.pass.types.JadxAfterLoadPass;
import jadx.api.plugins.pass.types.JadxPreparePass;

/**
 * Base interface for all jadx plugins
 * <br>
 * To create new plugin implement this interface and add to resources
 * a {@code META-INF/services/jadx.api.plugins.JadxPlugin} file with a full name of your class.
 */
public interface JadxPlugin {

	/**
	 * Method for provide plugin information, like name and description.
	 * Can be invoked several times.
	 */
	JadxPluginInfo getPluginInfo();

	/**
	 * Init plugin.
	 * Use {@link JadxPluginContext} to register passes, code inputs and options.
	 * For long operation, prefer {@link JadxPreparePass} or {@link JadxAfterLoadPass} instead.
	 */
	void init(JadxPluginContext context);
}
