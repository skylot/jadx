package jadx.api.impl.plugins;

import org.jetbrains.annotations.Nullable;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.api.plugins.pass.JadxPassContext;

public class SimplePluginContext implements JadxPluginContext {

	private final JadxDecompiler decompiler;
	private final JadxPassContext passContext;
	private @Nullable JadxGuiContext guiContext;

	public SimplePluginContext(JadxDecompiler decompiler) {
		this.decompiler = decompiler;
		this.passContext = new SimplePassContext(decompiler);
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
	public JadxPassContext getPassContext() {
		return passContext;
	}

	@Override
	public @Nullable JadxGuiContext getGuiContext() {
		return guiContext;
	}

	public void setGuiContext(JadxGuiContext guiContext) {
		this.guiContext = guiContext;
	}
}
