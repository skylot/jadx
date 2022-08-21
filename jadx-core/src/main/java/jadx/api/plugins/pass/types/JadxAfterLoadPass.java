package jadx.api.plugins.pass.types;

import jadx.api.JadxDecompiler;
import jadx.api.plugins.pass.JadxPass;

public interface JadxAfterLoadPass extends JadxPass {
	JadxPassType TYPE = new JadxPassType(JadxAfterLoadPass.class);

	void init(JadxDecompiler decompiler);

	@Override
	default JadxPassType getPassType() {
		return TYPE;
	}
}
