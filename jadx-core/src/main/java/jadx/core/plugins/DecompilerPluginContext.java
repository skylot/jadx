package jadx.core.plugins;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.data.IJadxFiles;
import jadx.api.plugins.data.IJadxPlugins;
import jadx.api.plugins.events.IJadxEvents;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.api.plugins.input.JadxCodeInput;
import jadx.api.plugins.options.JadxPluginOptions;
import jadx.api.plugins.pass.JadxPass;
import jadx.api.plugins.resources.IResourcesLoader;
import jadx.zip.ZipReader;

final class DecompilerPluginContext implements JadxPluginContext {
	private final JadxDecompiler decompiler;
	private final PluginContext pluginContext;

	DecompilerPluginContext(JadxDecompiler decompiler, PluginContext pluginContext) {
		this.decompiler = decompiler;
		this.pluginContext = pluginContext;
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
		pluginContext.addCodeInput(codeInput);
	}

	@Override
	public void registerOptions(JadxPluginOptions options) {
		pluginContext.registerOptions(options);
	}

	@Override
	public void registerInputsHashSupplier(Supplier<String> supplier) {
		pluginContext.registerInputsHashSupplier(supplier);
	}

	@Override
	public IResourcesLoader getResourcesLoader() {
		return decompiler.getResourcesLoader();
	}

	@Override
	public @Nullable JadxGuiContext getGuiContext() {
		return pluginContext.getGuiContext();
	}

	@Override
	public IJadxEvents events() {
		return decompiler.events();
	}

	@Override
	public IJadxPlugins plugins() {
		return pluginContext.plugins();
	}

	@Override
	public IJadxFiles files() {
		return pluginContext.files();
	}

	@Override
	public ZipReader getZipReader() {
		return decompiler.getZipReader();
	}

	@Override
	public String toString() {
		return pluginContext.toString();
	}
}
