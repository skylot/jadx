package jadx.core.dex.visitors;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.DebugChecks;

public class DepthTraversal {

	public static void visit(IDexTreeVisitor visitor, ClassNode cls) {
		try {
			if (visitor.visit(cls)) {
				cls.getInnerClasses().forEach(inCls -> visit(visitor, inCls));
				cls.getMethods().forEach(mth -> visit(visitor, mth));
			}
		} catch (StackOverflowError | Exception e) {
			cls.addError(e.getClass().getSimpleName() + " in pass: " + visitor.getClass().getSimpleName(), e);
		}
	}

	public static void visit(IDexTreeVisitor visitor, MethodNode mth) {
		try {
			if (mth.contains(AType.JADX_ERROR)) {
				return;
			}
			visitor.visit(mth);
			if (DebugChecks.checksEnabled) {
				DebugChecks.runChecksAfterVisitor(mth, visitor);
			}
		} catch (StackOverflowError | Exception e) {
			mth.addError(e.getClass().getSimpleName() + " in pass: " + visitor.getClass().getSimpleName(), e);
		}
	}

	private DepthTraversal() {
	}
}
