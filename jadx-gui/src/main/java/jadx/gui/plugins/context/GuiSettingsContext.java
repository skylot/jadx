package jadx.gui.plugins.context;

import java.util.List;

import jadx.api.plugins.gui.ISettingsGroup;
import jadx.api.plugins.gui.JadxGuiSettings;
import jadx.api.plugins.options.OptionDescription;
import jadx.gui.settings.ui.SubSettingsGroup;
import jadx.gui.settings.ui.plugins.PluginsSettings;
import jadx.gui.ui.MainWindow;

public class GuiSettingsContext implements JadxGuiSettings {
	private final GuiPluginContext guiPluginContext;

	public GuiSettingsContext(GuiPluginContext guiPluginContext) {
		this.guiPluginContext = guiPluginContext;
	}

	@Override
	public void setCustomSettingsGroup(ISettingsGroup group) {
		guiPluginContext.setCustomSettings(group);
	}

	@Override
	public ISettingsGroup buildSettingsGroupForOptions(String title, List<OptionDescription> options) {
		MainWindow mainWindow = guiPluginContext.getCommonContext().getMainWindow();
		PluginsSettings pluginsSettings = new PluginsSettings(mainWindow, mainWindow.getSettings());
		SubSettingsGroup settingsGroup = new SubSettingsGroup(title);
		pluginsSettings.addOptions(settingsGroup, options);
		return settingsGroup;
	}
}
