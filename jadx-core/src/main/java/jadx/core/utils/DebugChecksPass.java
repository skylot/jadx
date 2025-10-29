package jadx.core.utils;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.exceptions.JadxException;

public class DebugChecksPass extends AbstractVisitor {

	private final String visitorName;

	public DebugChecksPass(String visitorName) {
		this.visitorName = visitorName;
	}

	@Override
	public String getName() {
		return "Checks-for-" + visitorName;
	}

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (!mth.contains(AType.JADX_ERROR)) {
			try {
				DebugChecks.runChecksAfterVisitor(mth, visitorName);
			} catch (Exception e) {
				mth.addError("Check error", e);
			}
		}
	}
}
