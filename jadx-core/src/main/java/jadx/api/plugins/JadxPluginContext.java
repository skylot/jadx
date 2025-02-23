package jadx.api.plugins;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.plugins.data.IJadxFiles;
import jadx.api.plugins.data.IJadxPlugins;
import jadx.api.plugins.events.IJadxEvents;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.api.plugins.input.JadxCodeInput;
import jadx.api.plugins.options.JadxPluginOptions;
import jadx.api.plugins.pass.JadxPass;
import jadx.api.plugins.resources.IResourcesLoader;
import jadx.zip.ZipReader;

public interface JadxPluginContext {

	JadxArgs getArgs();

	JadxDecompiler getDecompiler();

	void addPass(JadxPass pass);

	void addCodeInput(JadxCodeInput codeInput);

	void registerOptions(JadxPluginOptions options);

	/**
	 * Function to calculate hash of all options which can change output code.
	 * Hash for input files ({@link JadxArgs#getInputFiles()}) and registered options
	 * calculated by default implementations.
	 */
	void registerInputsHashSupplier(Supplier<String> supplier);

	/**
	 * Customize resource loading
	 */
	IResourcesLoader getResourcesLoader();

	/**
	 * Access to jadx-gui specific methods
	 */
	@Nullable
	JadxGuiContext getGuiContext();

	/**
	 * Subscribe and send events
	 */
	IJadxEvents events();

	/**
	 * Access to registered plugins and runtime data
	 */
	IJadxPlugins plugins();

	/**
	 * Access to plugin specific files and directories
	 */
	IJadxFiles files();

	/**
	 * Custom jadx zip reader to fight tampering and provide additional security checks
	 */
	ZipReader getZipReader();
}
