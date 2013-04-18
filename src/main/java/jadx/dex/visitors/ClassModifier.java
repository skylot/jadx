package jadx.dex.visitors;

import jadx.dex.info.AccessInfo;
import jadx.dex.info.MethodInfo;
import jadx.dex.nodes.BlockNode;
import jadx.dex.nodes.ClassNode;
import jadx.dex.nodes.MethodNode;
import jadx.utils.exceptions.JadxException;

import java.util.Iterator;
import java.util.List;

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
				if (!isMethodUniq(cls, mth)) {
					// TODO add more checks before method deletion
					it.remove();
				}
			}

			// remove public empty constructors
			if (af.isConstructor()
					&& af.isPublic()
					&& mth.getArguments(false).isEmpty()) {
				List<BlockNode> bb = mth.getBasicBlocks();
				if (bb.isEmpty() || (bb.size() == 1 && bb.get(0).getInstructions().isEmpty())) {
					if (mth.getSuperCall() == null)
						it.remove();
				}
			}
		}
		return false;
	}

	private boolean isMethodUniq(ClassNode cls, MethodNode mth) {
		MethodInfo mi = mth.getMethodInfo();
		for (MethodNode otherMth : cls.getMethods()) {
			MethodInfo omi = otherMth.getMethodInfo();
			if (omi.getName().equals(mi.getName())
					&& otherMth != mth) {
				if (omi.getArgumentsTypes().size() == mi.getArgumentsTypes().size()) {
					// TODO: check to args objects types
					return false;
				}
			}
		}
		return true;
	}
}
