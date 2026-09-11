package jadx.api.gui.plugins;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import jadx.api.plugins.JadxPluginContext;

/**
 * Jadx-gui plugin of global scope:
 * - created and initialized with main window
 * - unloaded on main window destroy
 * - project init and unload events still can be used
 */
@SuppressWarnings("unused")
public abstract class JadxGlobalGuiPlugin extends JadxGuiPlugin {

	/**
	 * Main init method.
	 * Will be called after main window initialization.
	 */
	public abstract void pluginGlobalInit(@NotNull JadxGuiContextExt guiContext);

	public void globalInit(@NotNull JadxGuiContextExt guiContext) {
		guiContext.registerOptions(buildOptions());
		pluginGlobalInit(guiContext);
	}

	/**
	 * Project init method.
	 * Override to get project specific plugin context data
	 */
	@Override
	public void init(JadxPluginContext context, @NotNull JadxGuiContextExt guiContextExt) {
		// optional method
	}

	/**
	 * Rewrite base plugin logic to register option in global init
	 */
	@Override
	public void init(JadxPluginContext context) {
		init(context, (JadxGuiContextExt) Objects.requireNonNull(context.getGuiContext()));
	}

	/**
	 * Project close event.
	 * Can be used to release project related data.
	 */
	@Override
	public void unload() {
		// optional method
	}

	/**
	 * Main window close event, called before window destroy.
	 * It is suggested to not start any heavy operations.
	 * Any exceptions will be ignored.
	 */
	public void globalUnload() {
		// optional method
	}
}
