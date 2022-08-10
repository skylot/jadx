package jadx.api.impl.plugins;

import jadx.api.JadxDecompiler;
import jadx.api.plugins.pass.JadxPass;
import jadx.api.plugins.pass.JadxPassContext;

public class SimplePassContext implements JadxPassContext {

	private final JadxDecompiler jadxDecompiler;

	public SimplePassContext(JadxDecompiler jadxDecompiler) {
		this.jadxDecompiler = jadxDecompiler;
	}

	@Override
	public void addPass(JadxPass pass) {
		jadxDecompiler.addCustomPass(pass);
	}
}
