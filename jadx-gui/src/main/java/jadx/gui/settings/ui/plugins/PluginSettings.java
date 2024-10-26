package jadx.gui.settings.ui.plugins;

import java.awt.event.ItemEvent;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

import jadx.api.plugins.events.types.ReloadProject;
import jadx.api.plugins.gui.ISettingsGroup;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.api.plugins.options.JadxPluginOptions;
import jadx.api.plugins.options.OptionDescription;
import jadx.api.plugins.options.OptionFlag;
import jadx.api.plugins.options.OptionType;
import jadx.core.plugins.PluginContext;
import jadx.core.utils.Utils;
import jadx.gui.logs.LogOptions;
import jadx.gui.plugins.context.GuiPluginContext;
import jadx.gui.settings.JadxProject;
import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.ui.SettingsGroup;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;
import jadx.gui.utils.plugins.CollectPlugins;
import jadx.gui.utils.ui.DocumentUpdateListener;
import jadx.plugins.tools.JadxPluginsTools;
import jadx.plugins.tools.data.JadxPluginMetadata;
import jadx.plugins.tools.data.JadxPluginUpdate;

public class PluginSettings {
	private static final Logger LOG = LoggerFactory.getLogger(PluginSettings.class);

	private final MainWindow mainWindow;
	private final JadxSettings settings;

	public PluginSettings(MainWindow mainWindow, JadxSettings settings) {
		this.mainWindow = mainWindow;
		this.settings = settings;
	}

	public ISettingsGroup build() {
		List<PluginContext> collectedPlugins = new CollectPlugins(mainWindow).build();
		ISettingsGroup pluginsGroup = new PluginSettingsGroup(this, mainWindow, collectedPlugins);
		for (PluginContext context : collectedPlugins) {
			ISettingsGroup pluginGroup = addPluginGroup(context);
			if (pluginGroup != null) {
				pluginsGroup.getSubGroups().add(pluginGroup);
			}
		}
		return pluginsGroup;
	}

	public void addPlugin() {
		new InstallPluginDialog(mainWindow, this).setVisible(true);
	}

	private void requestReload() {
		mainWindow.events().send(ReloadProject.EVENT);
	}

	public void install(String locationId) {
		mainWindow.getBackgroundExecutor().execute(NLS.str("preferences.plugins.task.installing"),
				() -> {
					try {
						JadxPluginMetadata metadata = JadxPluginsTools.getInstance().install(locationId);
						LOG.info("Plugin installed: {}", metadata);
						requestReload();
					} catch (Exception e) {
						LOG.error("Install failed", e);
						mainWindow.showLogViewer(LogOptions.forLevel(Level.ERROR));
					}
				});
	}

	public void uninstall(String pluginId) {
		mainWindow.getBackgroundExecutor().execute(NLS.str("preferences.plugins.task.uninstalling"), () -> {
			boolean success = JadxPluginsTools.getInstance().uninstall(pluginId);
			if (success) {
				LOG.info("Uninstall complete");
				requestReload();
			} else {
				LOG.warn("Uninstall failed");
			}
		});
	}

	public void changeDisableStatus(String pluginId, boolean disabled) {
		mainWindow.getBackgroundExecutor().execute(
				NLS.str("preferences.plugins.task.status"),
				() -> JadxPluginsTools.getInstance().changeDisabledStatus(pluginId, disabled),
				s -> requestReload());
	}

	void updateAll() {
		mainWindow.getBackgroundExecutor().execute(NLS.str("preferences.plugins.task.updating"), () -> {
			List<JadxPluginUpdate> updates = JadxPluginsTools.getInstance().updateAll();
			if (!updates.isEmpty()) {
				LOG.info("Updates: {}\n  ", Utils.listToString(updates, "\n  "));
				requestReload();
			} else {
				LOG.info("No updates found");
			}
		});
	}

	private ISettingsGroup addPluginGroup(PluginContext context) {
		JadxGuiContext guiContext = context.getGuiContext();
		if (guiContext instanceof GuiPluginContext) {
			GuiPluginContext pluginGuiContext = (GuiPluginContext) guiContext;
			ISettingsGroup customSettingsGroup = pluginGuiContext.getCustomSettingsGroup();
			if (customSettingsGroup != null) {
				return customSettingsGroup;
			}
		}
		JadxPluginOptions options = context.getOptions();
		if (options == null) {
			return null;
		}
		List<OptionDescription> optionsDescriptions = options.getOptionsDescriptions();
		if (optionsDescriptions.isEmpty()) {
			return null;
		}
		SettingsGroup settingsGroup = new SettingsGroup(context.getPluginInfo().getName());
		addOptions(settingsGroup, optionsDescriptions);
		return settingsGroup;
	}

	public void addOptions(SettingsGroup pluginGroup, List<OptionDescription> optionsDescriptions) {
		for (OptionDescription opt : optionsDescriptions) {
			if (opt.getFlags().contains(OptionFlag.HIDE_IN_GUI)) {
				continue;
			}
			String optName = opt.name();
			String title = opt.description();
			Consumer<String> updateFunc;
			String curValue;
			if (opt.getFlags().contains(OptionFlag.PER_PROJECT)) {
				JadxProject project = mainWindow.getProject();
				updateFunc = value -> project.updatePluginOptions(m -> m.put(optName, value));
				curValue = project.getPluginOption(optName);
			} else {
				Map<String, String> optionsMap = settings.getPluginOptions();
				updateFunc = value -> optionsMap.put(optName, value);
				curValue = optionsMap.get(optName);
			}
			String value = curValue != null ? curValue : opt.defaultValue();

			JComponent editor = null;
			if (opt.values().isEmpty() || opt.getType() == OptionType.BOOLEAN) {
				try {
					editor = getPluginOptionEditor(opt, value, updateFunc);
				} catch (Exception e) {
					LOG.error("Failed to add editor for plugin option: {}", optName, e);
				}
			} else {
				JComboBox<String> combo = new JComboBox<>(opt.values().toArray(new String[0]));
				combo.setSelectedItem(value);
				combo.addActionListener(e -> updateFunc.accept((String) combo.getSelectedItem()));
				editor = combo;
			}
			if (editor != null) {
				JLabel label = pluginGroup.addRow(title, editor);
				boolean enabled = !opt.getFlags().contains(OptionFlag.DISABLE_IN_GUI);
				if (!enabled) {
					label.setEnabled(false);
					editor.setEnabled(false);
				}
			}
		}
	}

	private JComponent getPluginOptionEditor(OptionDescription opt, String value, Consumer<String> updateFunc) {
		switch (opt.getType()) {
			case STRING:
				JTextField textField = new JTextField();
				textField.setText(value == null ? "" : value);
				textField.getDocument().addDocumentListener(
						new DocumentUpdateListener(event -> updateFunc.accept(textField.getText())));
				return textField;

			case NUMBER:
				JSpinner numberField = new JSpinner();
				numberField.setValue(safeStringToInt(value, () -> safeStringToInt(opt.defaultValue(), () -> {
					throw new IllegalArgumentException("Failed to parse integer default value: " + opt.defaultValue());
				})));
				numberField.addChangeListener(e -> updateFunc.accept(numberField.getValue().toString()));
				return numberField;

			case BOOLEAN:
				JCheckBox boolField = new JCheckBox();
				boolField.setSelected(Objects.equals(value, "yes") || Objects.equals(value, "true"));
				boolField.addItemListener(e -> {
					boolean editorValue = e.getStateChange() == ItemEvent.SELECTED;
					updateFunc.accept(editorValue ? "yes" : "no");
				});
				return boolField;
		}
		return null;
	}

	private static int safeStringToInt(String value, IntSupplier defValueSupplier) {
		if (value == null) {
			return defValueSupplier.getAsInt();
		}
		try {
			return Integer.parseInt(value);
		} catch (Exception e) {
			LOG.warn("Failed parse string to int: {}", value, e);
			return defValueSupplier.getAsInt();
		}
	}
}
