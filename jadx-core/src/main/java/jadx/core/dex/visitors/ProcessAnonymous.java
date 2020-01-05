package jadx.core.dex.visitors;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;

@JadxVisitor(
		name = "ProcessAnonymous",
		desc = "Mark anonymous and lambda classes (for future inline)"
)
public class ProcessAnonymous extends AbstractVisitor {

	@Override
	public void init(RootNode root) {
		if (root.getArgs().isInlineAnonymousClasses()) {
			for (ClassNode cls : root.getClasses(true)) {
				markAnonymousClass(cls);
			}
		}
	}

	public static void runForClass(ClassNode cls) {
		if (cls.root().getArgs().isInlineAnonymousClasses()) {
			markAnonymousClass(cls);
		}
	}

	private static void markAnonymousClass(ClassNode cls) {
		if (isAnonymous(cls) || isLambdaCls(cls)) {
			cls.add(AFlag.ANONYMOUS_CLASS);
			cls.add(AFlag.DONT_GENERATE);

			for (MethodNode mth : cls.getMethods()) {
				if (mth.isConstructor()) {
					mth.add(AFlag.ANONYMOUS_CONSTRUCTOR);
				}
			}
		}
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
