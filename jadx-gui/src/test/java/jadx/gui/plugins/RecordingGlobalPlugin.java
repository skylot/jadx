package jadx.gui.plugins;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.api.gui.plugins.JadxGlobalGuiPlugin;
import jadx.api.gui.plugins.JadxGuiContextExt;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;
import jadx.api.plugins.gui.ISettingsGroup;
import jadx.api.plugins.options.JadxPluginOptions;
import jadx.api.plugins.options.impl.BasePluginOptionsBuilder;

public class RecordingGlobalPlugin extends JadxGlobalGuiPlugin {
	private final String pluginId;

	@Nullable
	ISettingsGroup customSettings;

	@Nullable
	JadxGuiContextExt globalContext;
	final List<JadxPluginContext> projectContexts = new ArrayList<>();
	String optionValue;

	int globalInitCount;
	int projectInitCount;
	int unloadCount;
	int globalUnloadCount;

	public RecordingGlobalPlugin(String pluginId) {
		this.pluginId = pluginId;
	}

	String getOptionName() {
		return pluginId + ".value";
	}

	@Override
	public JadxPluginInfo getPluginInfo() {
		return JadxPluginInfoBuilder.pluginId(pluginId).name(pluginId).description("test").build();
	}

	@Override
	public JadxPluginOptions buildOptions() {
		return new BasePluginOptionsBuilder() {
			@Override
			public void registerOptions() {
				strOption(getOptionName())
						.description("test option")
						.defaultValue("default")
						.setter(v -> optionValue = v);
			}
		};
	}

	@Override
	public void pluginGlobalInit(@NotNull JadxGuiContextExt guiContext) {
		globalInitCount++;
		globalContext = guiContext;
		if (customSettings != null) {
			guiContext.settings().setCustomSettingsGroup(customSettings);
		}
	}

	@Override
	public void init(JadxPluginContext context, @NotNull JadxGuiContextExt guiContextExt) {
		projectInitCount++;
		projectContexts.add(context);
	}

	@Override
	public void unload() {
		unloadCount++;
	}

	@Override
	public void globalUnload() {
		globalUnloadCount++;
	}
}
