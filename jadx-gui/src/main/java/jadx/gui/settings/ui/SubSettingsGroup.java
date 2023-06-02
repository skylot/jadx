package jadx.gui.settings.ui;

import java.util.ArrayList;
import java.util.List;

import jadx.api.plugins.gui.ISettingsGroup;

public class SubSettingsGroup extends SettingsGroup {

	private final List<ISettingsGroup> groups = new ArrayList<>();

	public SubSettingsGroup(String title) {
		super(title);
	}

	@Override
	public List<ISettingsGroup> getSubGroups() {
		return groups;
	}
}
