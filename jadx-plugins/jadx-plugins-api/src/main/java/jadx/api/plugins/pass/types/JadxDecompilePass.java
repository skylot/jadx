package jadx.api.plugins.pass.types;

import jadx.api.core.nodes.IClassNode;
import jadx.api.core.nodes.IMethodNode;
import jadx.api.core.nodes.IRootNode;
import jadx.api.plugins.pass.JadxPass;

public interface JadxDecompilePass extends JadxPass {
	JadxPassType TYPE = new JadxPassType(JadxDecompilePass.class);

	void init(IRootNode root);

	/**
	 * Visit class
	 *
	 * @return false for disable child methods and inner classes traversal
	 */
	boolean visit(IClassNode cls);

	/**
	 * Visit method
	 */
	void visit(IMethodNode mth);

	@Override
	default JadxPassType getPassType() {
		return TYPE;
	}
}
