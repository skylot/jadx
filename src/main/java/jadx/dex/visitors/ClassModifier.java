package jadx.dex.visitors;

import jadx.dex.info.AccessInfo;
import jadx.dex.nodes.ClassNode;
import jadx.dex.nodes.MethodNode;
import jadx.utils.exceptions.JadxException;

import java.util.Iterator;

public class ClassModifier extends AbstractVisitor {

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		for (ClassNode inner : cls.getInnerClasses()) {
			visit(inner);
		}

		for (Iterator<MethodNode> it = cls.getMethods().iterator(); it.hasNext();) {
			MethodNode mth = it.next();
			AccessInfo af = mth.getAccessFlags();

			// remove bridge methods
			if (af.isBridge() && af.isSynthetic()) {
				// TODO make some checks before deleting
				it.remove();
			}
		}
		return false;
	}
}
