package jadx.dex.visitors;

import jadx.dex.nodes.ClassNode;
import jadx.dex.nodes.MethodNode;
import jadx.utils.ErrorsCounter;

public class DepthTraverser {

	public static void visit(IDexTreeVisitor visitor, ClassNode cls) {
		try {
			if (visitor.visit(cls)) {
				for (ClassNode inCls : cls.getInnerClasses())
					visit(visitor, inCls);
				for (MethodNode mth : cls.getMethods())
					visit(visitor, mth);
			}
		} catch (Throwable e) {
			ErrorsCounter.classError(cls,
					e.getClass().getSimpleName() + " in pass: " + visitor.getClass().getSimpleName(), e);
		}
	}

	public static void visit(IDexTreeVisitor visitor, MethodNode mth) {
		try {
			visitor.visit(mth);
		} catch (Throwable e) {
			ErrorsCounter.methodError(mth,
					e.getClass().getSimpleName() + " in pass: " + visitor.getClass().getSimpleName(), e);
		}
	}
}
