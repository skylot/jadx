package jadx.api.gui.plugins;

import org.jetbrains.annotations.NotNull;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.gui.IJadxGuiPlugin;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.api.plugins.options.JadxPluginOptions;
import jadx.api.plugins.options.impl.NoPluginOptions;

/**
 * Simplified jadx-gui plugin base class.
 * Initializing only in jadx-gui and correctly build plugin options for show in preferences window.
 */
@SuppressWarnings("unused")
public abstract class JadxGuiPlugin implements JadxPlugin, IJadxGuiPlugin {

	/**
	 * Implement to init plugin logic.
	 *
	 * @param context       - base plugin context
	 * @param guiContextExt - extended context for jadx-gui API usage
	 */
	public abstract void init(JadxPluginContext context, @NotNull JadxGuiContextExt guiContextExt);

	/**
	 * Optional method.
	 * Override to add plugin options into preferences
	 */
	public JadxPluginOptions buildOptions() {
		return NoPluginOptions.INSTANCE;
	}

	@Override
	public void init(JadxPluginContext context) {
		context.registerOptions(buildOptions());
		JadxGuiContext guiContext = context.getGuiContext();
		if (guiContext instanceof JadxGuiContextExt) {
			init(context, (JadxGuiContextExt) guiContext);
		}
	}
}
