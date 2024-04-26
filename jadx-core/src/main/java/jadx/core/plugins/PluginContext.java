package jadx.core.plugins;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.data.IJadxPlugins;
import jadx.api.plugins.data.JadxPluginRuntimeData;
import jadx.api.plugins.events.IJadxEvents;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.api.plugins.input.ICodeLoader;
import jadx.api.plugins.input.JadxCodeInput;
import jadx.api.plugins.input.data.impl.MergeCodeLoader;
import jadx.api.plugins.options.JadxPluginOptions;
import jadx.api.plugins.options.OptionDescription;
import jadx.api.plugins.options.OptionFlag;
import jadx.api.plugins.pass.JadxPass;
import jadx.api.plugins.resources.IResourcesLoader;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;

public class PluginContext implements JadxPluginContext, JadxPluginRuntimeData, Comparable<PluginContext> {
	private final JadxDecompiler decompiler;
	private final JadxPluginsData pluginsData;
	private final JadxPlugin plugin;
	private final JadxPluginInfo pluginInfo;
	private @Nullable JadxGuiContext guiContext;

	private final List<JadxCodeInput> codeInputs = new ArrayList<>();
	private @Nullable JadxPluginOptions options;
	private @Nullable Supplier<String> inputsHashSupplier;

	private boolean initialized;

	PluginContext(JadxDecompiler decompiler, JadxPluginsData pluginsData, JadxPlugin plugin) {
		this.decompiler = decompiler;
		this.pluginsData = pluginsData;
		this.plugin = plugin;
		this.pluginInfo = plugin.getPluginInfo();
	}

	void init() {
		plugin.init(this);
		initialized = true;
	}

	@Override
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

	@Override
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

	@Override
	public String getInputsHash() {
		if (inputsHashSupplier == null) {
			return defaultOptionsHash();
		}
		try {
			return inputsHashSupplier.get();
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to get inputs hash for plugin: " + getPluginId(), e);
		}
	}

	private String defaultOptionsHash() {
		if (options == null) {
			return "";
		}
		Map<String, String> allOptions = getArgs().getPluginOptions();
		StringBuilder sb = new StringBuilder();
		for (OptionDescription optDesc : options.getOptionsDescriptions()) {
			if (!optDesc.getFlags().contains(OptionFlag.NOT_CHANGING_CODE)) {
				sb.append(':').append(allOptions.get(optDesc.name()));
			}
		}
		return FileUtils.md5Sum(sb.toString());
	}

	@Override
	public IJadxEvents events() {
		return decompiler.events();
	}

	@Override
	public IResourcesLoader getResourcesLoader() {
		return decompiler.getResourcesLoader();
	}

	@Override
	public @Nullable JadxGuiContext getGuiContext() {
		return guiContext;
	}

	public void setGuiContext(JadxGuiContext guiContext) {
		this.guiContext = guiContext;
	}

	@Override
	public JadxPlugin getPluginInstance() {
		return plugin;
	}

	@Override
	public JadxPluginInfo getPluginInfo() {
		return pluginInfo;
	}

	@Override
	public String getPluginId() {
		return pluginInfo.getPluginId();
	}

	@Override
	public @Nullable JadxPluginOptions getOptions() {
		return options;
	}

	@Override
	public IJadxPlugins plugins() {
		return pluginsData;
	}

	@Override
	public ICodeLoader loadCodeFiles(List<Path> files, @Nullable Closeable closeable) {
		return new MergeCodeLoader(
				Utils.collectionMap(codeInputs, codeInput -> codeInput.loadFiles(files)),
				closeable);
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
