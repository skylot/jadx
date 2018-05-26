package jadx.core.dex.visitors;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.ErrorsCounter;

public class DepthTraversal {

	public static void visit(IDexTreeVisitor visitor, ClassNode cls) {
		try {
			if (visitor.visit(cls)) {
				cls.getInnerClasses().forEach(inCls -> visit(visitor, inCls));
				cls.getMethods().forEach(mth -> visit(visitor, mth));
			}
		} catch (Exception e) {
			ErrorsCounter.classError(cls,
					e.getClass().getSimpleName() + " in pass: " + visitor.getClass().getSimpleName(), e);
		}
	}

	public static void visit(IDexTreeVisitor visitor, MethodNode mth) {
		if (mth.contains(AType.JADX_ERROR)) {
			return;
		}
		try {
			visitor.visit(mth);
		} catch (Exception e) {
			ErrorsCounter.methodError(mth,
					e.getClass().getSimpleName() + " in pass: " + visitor.getClass().getSimpleName(), e);
		}
	}

	private DepthTraversal() {
	}
}
