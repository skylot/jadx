package jadx.api.plugins;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.api.plugins.input.JadxCodeInput;
import jadx.api.plugins.options.JadxPluginOptions;
import jadx.api.plugins.pass.JadxPass;

public interface JadxPluginContext {

	JadxArgs getArgs();

	JadxDecompiler getDecompiler();

	void addPass(JadxPass pass);

	void addCodeInput(JadxCodeInput codeInput);

	void registerOptions(JadxPluginOptions options);

	/**
	 * Function to calculate hash of all options which can change output code.
	 * Hash for input files ({@link JadxArgs#getInputFiles()}) already calculated,
	 * so this method can omit these files.
	 */
	void registerInputsHashSupplier(Supplier<String> supplier);

	@Nullable
	JadxGuiContext getGuiContext();
}
