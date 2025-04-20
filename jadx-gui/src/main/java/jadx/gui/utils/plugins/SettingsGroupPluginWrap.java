package jadx.gui.utils.plugins;

import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.gui.ISettingsGroup;

public class SettingsGroupPluginWrap implements ISettingsGroup {
	private static final Logger LOG = LoggerFactory.getLogger(SettingsGroupPluginWrap.class);

	private final String pluginId;
	private final ISettingsGroup pluginSettingGroup;

	public SettingsGroupPluginWrap(String pluginId, ISettingsGroup pluginSettingGroup) {
		this.pluginId = pluginId;
		this.pluginSettingGroup = pluginSettingGroup;
	}

	@Override
	public String getTitle() {
		try {
			return pluginSettingGroup.getTitle();
		} catch (Throwable t) {
			LOG.warn("Failed to get settings group title for plugin: {}", pluginId, t);
			return "<error>";
		}
	}

	@Override
	public JComponent buildComponent() {
		try {
			return pluginSettingGroup.buildComponent();
		} catch (Throwable t) {
			LOG.warn("Failed to build settings group component for plugin: {}", pluginId, t);
			return new JLabel("<error>");
		}
	}

	@Override
	public List<ISettingsGroup> getSubGroups() {
		try {
			return pluginSettingGroup.getSubGroups();
		} catch (Throwable t) {
			LOG.warn("Failed to get settings group sub-groups for plugin: {}", pluginId, t);
			return Collections.emptyList();
		}
	}

	@Override
	public void close(boolean save) {
		try {
			pluginSettingGroup.close(save);
		} catch (Throwable t) {
			LOG.warn("Failed to close settings group for plugin: {}", pluginId, t);
		}
	}
}
