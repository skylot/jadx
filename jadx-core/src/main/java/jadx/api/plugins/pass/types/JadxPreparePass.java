package jadx.api.plugins.pass.types;

import jadx.api.plugins.pass.JadxPass;
import jadx.core.dex.nodes.RootNode;

public interface JadxPreparePass extends JadxPass {
	JadxPassType TYPE = new JadxPassType(JadxPreparePass.class);

	void init(RootNode root);

	@Override
	default JadxPassType getPassType() {
		return TYPE;
	}
}
