package jadx.api.plugins;

import org.jetbrains.annotations.Nullable;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.api.plugins.pass.JadxPassContext;

public interface JadxPluginContext {

	JadxArgs getArgs();

	JadxDecompiler getDecompiler();

	JadxPassContext getPassContext();

	@Nullable
	JadxGuiContext getGuiContext();
}
