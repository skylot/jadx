package jadx.api.plugins.pass;

import jadx.api.plugins.pass.types.JadxPassType;

public interface JadxPass {
	JadxPassInfo getInfo();

	JadxPassType getPassType();
}
