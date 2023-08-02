package jadx.gui.settings.ui.shortcut;

import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jadx.api.plugins.gui.ISettingsGroup;
import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.ui.JadxSettingsWindow;
import jadx.gui.settings.ui.SettingsGroup;
import jadx.gui.ui.action.ActionCategory;
import jadx.gui.ui.action.ActionModel;
import jadx.gui.utils.NLS;
import jadx.gui.utils.shortcut.Shortcut;

public class ShortcutsSettingsGroup implements ISettingsGroup {
	private final JadxSettingsWindow settingsWindow;
	private final JadxSettings settings;

	public ShortcutsSettingsGroup(JadxSettingsWindow settingsWindow, JadxSettings settings) {
		this.settingsWindow = settingsWindow;
		this.settings = settings;
	}

	@Override
	public String getTitle() {
		return NLS.str("preferences.shortcuts");
	}

	@Override
	public JComponent buildComponent() {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(new JLabel(NLS.str("preferences.select_shortcuts")), BorderLayout.NORTH);
		return panel;
	}

	@Override
	public List<ISettingsGroup> getSubGroups() {
		return Arrays.stream(ActionCategory.values())
				.map(this::makeShortcutsGroup)
				.collect(Collectors.toUnmodifiableList());
	}

	private SettingsGroup makeShortcutsGroup(ActionCategory category) {
		SettingsGroup group = new SettingsGroup(category.getName());
		for (ActionModel actionModel : ActionModel.select(category)) {
			Shortcut shortcut = settings.getShortcuts().get(actionModel);
			ShortcutEdit edit = new ShortcutEdit(actionModel, settingsWindow, settings);
			edit.setShortcut(shortcut);
			group.addRow(actionModel.getName(), edit);
		}
		return group;
	}
}
