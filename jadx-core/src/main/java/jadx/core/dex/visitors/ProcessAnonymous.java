package jadx.core.dex.visitors;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.usage.UsageInfoVisitor;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "ProcessAnonymous",
		desc = "Mark anonymous and lambda classes (for future inline)",
		runAfter = {
				UsageInfoVisitor.class
		}
)
public class ProcessAnonymous extends AbstractVisitor {

	private boolean inlineAnonymous;

	@Override
	public void init(RootNode root) {
		inlineAnonymous = root.getArgs().isInlineAnonymousClasses();
	}

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		if (!inlineAnonymous) {
			return false;
		}
		markAnonymousClass(cls);
		return true;
	}

	private static void markAnonymousClass(ClassNode cls) {
		if (usedOnlyOnce(cls) || isAnonymous(cls) || isLambdaCls(cls)) {
			cls.add(AFlag.ANONYMOUS_CLASS);
			cls.add(AFlag.DONT_GENERATE);

			for (MethodNode mth : cls.getMethods()) {
				if (mth.isConstructor()) {
					mth.add(AFlag.ANONYMOUS_CONSTRUCTOR);
				}
			}
		}
	}

	private static boolean usedOnlyOnce(ClassNode cls) {
		if (cls.getUseIn().size() == 1 && cls.getUseInMth().size() == 1) {
			// used only once
			boolean synthetic = cls.getAccessFlags().isSynthetic() || cls.getClassInfo().getShortName().contains("$");
			if (synthetic) {
				// must have only one constructor which used only once
				MethodNode ctr = null;
				for (MethodNode mth : cls.getMethods()) {
					if (mth.isConstructor()) {
						if (ctr != null) {
							ctr = null;
							break;
						}
						ctr = mth;
					}
				}
				return ctr != null && ctr.getUseIn().size() == 1;
			}
		}
		return false;
	}

	private static boolean isAnonymous(ClassNode cls) {
		return cls.getClassInfo().isInner()
				&& Character.isDigit(cls.getClassInfo().getShortName().charAt(0))
				&& cls.getMethods().stream().filter(MethodNode::isConstructor).count() == 1;
	}

	private static boolean isLambdaCls(ClassNode cls) {
		return cls.getAccessFlags().isSynthetic()
				&& cls.getAccessFlags().isFinal()
				&& cls.getClassInfo().getRawName().contains(".-$$Lambda$")
				&& countStaticFields(cls) == 0;
	}

	private static int countStaticFields(ClassNode cls) {
		int c = 0;
		for (FieldNode field : cls.getFields()) {
			if (field.getAccessFlags().isStatic()) {
				c++;
			}
		}
		return c;
	}
}
