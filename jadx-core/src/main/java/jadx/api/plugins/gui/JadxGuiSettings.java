package jadx.api.plugins.gui;

import java.util.List;

import jadx.api.plugins.options.OptionDescription;

public interface JadxGuiSettings {

	/**
	 * Set plugin custom settings page
	 */
	void setCustomSettings(ISettingsGroup group);

	/**
	 * Helper method to build options group only for provided option list
	 */
	ISettingsGroup buildSettingsGroupForOptions(String title, List<OptionDescription> options);
}
