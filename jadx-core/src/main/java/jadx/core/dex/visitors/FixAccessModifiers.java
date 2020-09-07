package jadx.core.dex.visitors;

import jadx.api.plugins.input.data.AccessFlags;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.ICodeNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "FixAccessModifiers",
		desc = "Change class and method access modifiers if needed",
		runAfter = ModVisitor.class
)
public class FixAccessModifiers extends AbstractVisitor {

	private boolean respectAccessModifiers;

	@Override
	public void init(RootNode root) {
		this.respectAccessModifiers = root.getArgs().isRespectBytecodeAccModifiers();
	}

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		if (respectAccessModifiers) {
			return true;
		}
		int newVisFlag = fixClassVisibility(cls);
		if (newVisFlag != -1) {
			changeVisibility(cls, newVisFlag);
		}
		return true;
	}

	@Override
	public void visit(MethodNode mth) {
		if (respectAccessModifiers) {
			return;
		}
		int newVisFlag = fixMethodVisibility(mth);
		if (newVisFlag != -1) {
			changeVisibility(mth, newVisFlag);
		}
	}

	public static void changeVisibility(ICodeNode node, int newVisFlag) {
		AccessInfo accessFlags = node.getAccessFlags();
		AccessInfo newAccFlags = accessFlags.changeVisibility(newVisFlag);
		if (newAccFlags != accessFlags) {
			node.setAccessFlags(newAccFlags);
			node.addAttr(AType.COMMENTS, "access modifiers changed from: " + accessFlags.visibilityName());
		}
	}

	private int fixClassVisibility(ClassNode cls) {
		if (cls.getUseIn().isEmpty()) {
			return -1;
		}
		AccessInfo accessFlags = cls.getAccessFlags();
		if (accessFlags.isPrivate()) {
			if (!cls.isInner()) {
				return AccessFlags.PUBLIC;
			}
			// check if private inner class is used outside
			ClassNode topParentClass = cls.getTopParentClass();
			for (ClassNode useCls : cls.getUseIn()) {
				if (useCls.getTopParentClass() != topParentClass) {
					return AccessFlags.PUBLIC;
				}
			}
		}
		if (accessFlags.isPackagePrivate()) {
			String pkg = cls.getPackage();
			for (ClassNode useCls : cls.getUseIn()) {
				if (!useCls.getPackage().equals(pkg)) {
					return AccessFlags.PUBLIC;
				}
			}
		}
		if (!accessFlags.isPublic()) {
			// if class is used in inlinable method => make it public
			for (MethodNode useMth : cls.getUseInMth()) {
				boolean canInline = MarkMethodsForInline.canInline(useMth) || useMth.contains(AType.METHOD_INLINE);
				if (canInline && !useMth.getUseIn().isEmpty()) {
					return AccessFlags.PUBLIC;
				}
			}
		}
		return -1;
	}

	private static int fixMethodVisibility(MethodNode mth) {
		if (mth.isVirtual()) {
			// make virtual methods public
			return AccessFlags.PUBLIC;
		} else {
			AccessInfo accessFlags = mth.getAccessFlags();
			if (accessFlags.isAbstract()) {
				// make abstract methods public
				return AccessFlags.PUBLIC;
			}
			// enum constructor can't be public
			if (accessFlags.isConstructor()
					&& accessFlags.isPublic()
					&& mth.getParentClass().isEnum()) {
				return 0;
			}
			if (accessFlags.isConstructor() || accessFlags.isStatic()) {
				// TODO: make public if used outside
				return -1;
			}
			// make other direct methods private
			return AccessFlags.PRIVATE;
		}
	}
}
