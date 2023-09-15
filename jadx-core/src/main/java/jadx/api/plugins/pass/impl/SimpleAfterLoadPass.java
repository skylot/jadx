package jadx.api.plugins.pass.impl;

import java.util.function.Consumer;

import jadx.api.JadxDecompiler;
import jadx.api.plugins.pass.JadxPassInfo;
import jadx.api.plugins.pass.types.JadxAfterLoadPass;

public class SimpleAfterLoadPass implements JadxAfterLoadPass {

	private final JadxPassInfo info;
	private final Consumer<JadxDecompiler> init;

	public SimpleAfterLoadPass(String name, Consumer<JadxDecompiler> init) {
		this.info = new SimpleJadxPassInfo(name);
		this.init = init;
	}

	@Override
	public JadxPassInfo getInfo() {
		return info;
	}

	@Override
	public void init(JadxDecompiler decompiler) {
		init.accept(decompiler);
	}
}
