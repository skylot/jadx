package jadx.api.impl.plugins;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.api.plugins.input.JadxCodeInput;
import jadx.api.plugins.options.JadxPluginOptions;
import jadx.api.plugins.pass.JadxPass;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class PluginsContext implements JadxPluginContext {

	private final JadxDecompiler decompiler;
	private final List<JadxCodeInput> codeInputs = new ArrayList<>();
	private final Map<JadxPlugin, JadxPluginOptions> optionsMap = new IdentityHashMap<>();
	private @Nullable JadxGuiContext guiContext;

	private @Nullable JadxPlugin currentPlugin;

	public PluginsContext(JadxDecompiler decompiler) {
		this.decompiler = decompiler;
	}

	@Override
	public JadxArgs getArgs() {
		return decompiler.getArgs();
	}

	@Override
	public JadxDecompiler getDecompiler() {
		return decompiler;
	}

	@Override
	public void addPass(JadxPass pass) {
		decompiler.addCustomPass(pass);
	}

	@Override
	public void addCodeInput(JadxCodeInput codeInput) {
		codeInputs.add(codeInput);
	}

	public List<JadxCodeInput> getCodeInputs() {
		return codeInputs;
	}

	public void setCurrentPlugin(JadxPlugin currentPlugin) {
		this.currentPlugin = currentPlugin;
	}

	@Override
	public void registerOptions(JadxPluginOptions options) {
		Objects.requireNonNull(currentPlugin);
		try {
			options.setOptions(decompiler.getArgs().getPluginOptions());
			optionsMap.put(currentPlugin, options);
		} catch (Exception e) {
			String pluginId = currentPlugin.getPluginInfo().getPluginId();
			throw new JadxRuntimeException("Failed to apply options for plugin: " + pluginId, e);
		}
	}

	public Map<JadxPlugin, JadxPluginOptions> getOptionsMap() {
		return optionsMap;
	}

	@Override
	public @Nullable JadxGuiContext getGuiContext() {
		return guiContext;
	}

	public void setGuiContext(JadxGuiContext guiContext) {
		this.guiContext = guiContext;
	}
}
