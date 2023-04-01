package jadx.api.plugins.pass.types;

import jadx.api.plugins.pass.JadxPass;
import jadx.core.dex.nodes.RootNode;

public interface JadxPreparePass extends JadxPass {
	JadxPassType TYPE = new JadxPassType("PreparePass");

	void init(RootNode root);

	@Override
	default JadxPassType getPassType() {
		return TYPE;
	}
}
