package jadx.gui.plugins.context;

import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.metadata.ICodeNodeRef;
import jadx.api.plugins.gui.ISettingsGroup;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.api.plugins.gui.JadxGuiSettings;
import jadx.core.plugins.PluginContext;
import jadx.gui.utils.UiUtils;

public class GuiPluginContext implements JadxGuiContext {
	private static final Logger LOG = LoggerFactory.getLogger(GuiPluginContext.class);

	private final CommonGuiPluginsContext commonContext;
	private final PluginContext pluginContext;

	private @Nullable ISettingsGroup customSettingsGroup;

	public GuiPluginContext(CommonGuiPluginsContext commonContext, PluginContext pluginContext) {
		this.commonContext = commonContext;
		this.pluginContext = pluginContext;
	}

	public CommonGuiPluginsContext getCommonContext() {
		return commonContext;
	}

	public PluginContext getPluginContext() {
		return pluginContext;
	}

	@Override
	public void uiRun(Runnable runnable) {
		UiUtils.uiRun(runnable);
	}

	@Override
	public void addMenuAction(String name, Runnable action) {
		commonContext.addMenuAction(name, action);
	}

	@Override
	public void addPopupMenuAction(String name, @Nullable Function<ICodeNodeRef, Boolean> enabled,
			@Nullable String keyBinding, Consumer<ICodeNodeRef> action) {
		commonContext.getCodePopupActionList().add(new CodePopupAction(name, enabled, keyBinding, action));
	}

	@Override
	public boolean registerGlobalKeyBinding(String id, String keyBinding, Runnable action) {
		KeyStroke keyStroke = KeyStroke.getKeyStroke(keyBinding);
		if (keyStroke == null) {
			throw new IllegalArgumentException("Failed to parse key binding: " + keyBinding);
		}
		JPanel mainPanel = (JPanel) commonContext.getMainWindow().getContentPane();
		Object prevBinding = mainPanel.getInputMap().get(keyStroke);
		if (prevBinding != null) {
			return false;
		}
		UiUtils.addKeyBinding(mainPanel, keyStroke, id, action);
		return true;
	}

	@Override
	public void copyToClipboard(String str) {
		UiUtils.copyToClipboard(str);
	}

	@Override
	public JadxGuiSettings settings() {
		return new GuiSettingsContext(this);
	}

	void setCustomSettings(ISettingsGroup customSettingsGroup) {
		this.customSettingsGroup = customSettingsGroup;
	}

	public @Nullable ISettingsGroup getCustomSettingsGroup() {
		return customSettingsGroup;
	}
}
