package jadx.dex.visitors;

import jadx.dex.nodes.ClassNode;
import jadx.dex.nodes.MethodNode;
import jadx.utils.exceptions.JadxException;

public class AbstractVisitor implements IDexTreeVisitor {

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		return true;
	}

	@Override
	public void visit(MethodNode mth) throws JadxException {
	}

}
