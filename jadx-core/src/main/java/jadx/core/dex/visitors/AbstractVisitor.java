package jadx.core.dex.visitors;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxException;

public class AbstractVisitor implements IDexTreeVisitor {

	@Override
	public void init(RootNode root) throws JadxException {
		// no op implementation
	}

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		// no op implementation
		return true;
	}

	@Override
	public void visit(MethodNode mth) throws JadxException {
		// no op implementation
	}
}
