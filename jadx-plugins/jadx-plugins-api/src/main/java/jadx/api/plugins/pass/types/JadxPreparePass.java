package jadx.api.plugins.pass.types;

import jadx.api.core.nodes.IRootNode;
import jadx.api.plugins.pass.JadxPass;

public interface JadxPreparePass extends JadxPass {
	JadxPassType TYPE = new JadxPassType(JadxPreparePass.class);

	void init(IRootNode root);

	@Override
	default JadxPassType getPassType() {
		return TYPE;
	}
}
