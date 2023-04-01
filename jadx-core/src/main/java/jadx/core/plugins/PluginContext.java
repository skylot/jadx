package jadx.core.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.api.plugins.input.JadxCodeInput;
import jadx.api.plugins.options.JadxPluginOptions;
import jadx.api.plugins.pass.JadxPass;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class PluginContext implements JadxPluginContext, Comparable<PluginContext> {
	private final JadxDecompiler decompiler;
	private final JadxPlugin plugin;
	private final JadxPluginInfo pluginInfo;
	private @Nullable JadxGuiContext guiContext;

	private final List<JadxCodeInput> codeInputs = new ArrayList<>();
	private @Nullable JadxPluginOptions options;
	private @Nullable Supplier<String> inputsHashSupplier;

	private boolean initialized;

	PluginContext(JadxDecompiler decompiler, JadxPlugin plugin) {
		this.decompiler = decompiler;
		this.plugin = plugin;
		this.pluginInfo = plugin.getPluginInfo();
	}

	void init() {
		plugin.init(this);
		initialized = true;
	}

	public boolean isInitialized() {
		return initialized;
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
		this.codeInputs.add(codeInput);
	}

	public List<JadxCodeInput> getCodeInputs() {
		return codeInputs;
	}

	@Override
	public void registerOptions(JadxPluginOptions options) {
		try {
			this.options = Objects.requireNonNull(options);
			options.setOptions(getArgs().getPluginOptions());
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to apply options for plugin: " + getPluginId(), e);
		}
	}

	@Override
	public void registerInputsHashSupplier(Supplier<String> supplier) {
		this.inputsHashSupplier = supplier;
	}

	public String getInputsHash() {
		if (inputsHashSupplier != null) {
			try {
				return inputsHashSupplier.get();
			} catch (Exception e) {
				throw new JadxRuntimeException("Failed to get inputs hash for plugin: " + getPluginId(), e);
			}
		}
		return "";
	}

	@Override
	public @Nullable JadxGuiContext getGuiContext() {
		return guiContext;
	}

	public void setGuiContext(JadxGuiContext guiContext) {
		this.guiContext = guiContext;
	}

	public JadxPlugin getPlugin() {
		return plugin;
	}

	public JadxPluginInfo getPluginInfo() {
		return pluginInfo;
	}

	public String getPluginId() {
		return pluginInfo.getPluginId();
	}

	public JadxPluginOptions getOptions() {
		return options;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof PluginContext)) {
			return false;
		}
		return this.getPluginId().equals(((PluginContext) other).getPluginId());
	}

	@Override
	public int hashCode() {
		return getPluginId().hashCode();
	}

	@Override
	public int compareTo(PluginContext other) {
		return this.getPluginId().compareTo(other.getPluginId());
	}

	@Override
	public String toString() {
		return getPluginId();
	}
}
