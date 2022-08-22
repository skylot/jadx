package jadx.api.plugins;

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

	@Nullable
	JadxGuiContext getGuiContext();
}
