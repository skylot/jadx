package jadx.api.plugins.pass.types;

import jadx.api.core.nodes.IJadxDecompiler;
import jadx.api.plugins.pass.JadxPass;

public interface JadxAfterLoadPass extends JadxPass {
	JadxPassType TYPE = new JadxPassType(JadxAfterLoadPass.class);

	void init(IJadxDecompiler decompiler);

	@Override
	default JadxPassType getPassType() {
		return TYPE;
	}
}
