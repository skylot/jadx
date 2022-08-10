package jadx.api.plugins;

import org.jetbrains.annotations.Nullable;

import jadx.api.core.nodes.IJadxDecompiler;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.api.plugins.pass.JadxPassContext;

public interface JadxPluginContext {

	IJadxDecompiler getDecompiler();

	JadxPassContext getPassContext();

	@Nullable
	JadxGuiContext getGuiContext();
}
