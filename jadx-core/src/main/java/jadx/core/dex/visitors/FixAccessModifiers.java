package jadx.core.dex.visitors;

import jadx.api.plugins.input.data.AccessFlags;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.MethodOverrideAttr;
import jadx.core.dex.attributes.nodes.NotificationAttrNode;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.IMethodDetails;
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
		if (respectAccessModifiers || mth.contains(AFlag.DONT_GENERATE)) {
			return;
		}
		int newVisFlag = fixMethodVisibility(mth);
		if (newVisFlag != -1) {
			changeVisibility(mth, newVisFlag);
		}
	}

	public static void changeVisibility(NotificationAttrNode node, int newVisFlag) {
		AccessInfo accessFlags = node.getAccessFlags();
		AccessInfo newAccFlags = accessFlags.changeVisibility(newVisFlag);
		if (newAccFlags != accessFlags) {
			node.setAccessFlags(newAccFlags);
			node.addInfoComment("Access modifiers changed from: " + accessFlags.visibilityName());
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
				boolean canInline = useMth.contains(AFlag.METHOD_CANDIDATE_FOR_INLINE) || useMth.contains(AType.METHOD_INLINE);
				if (canInline && !useMth.getUseIn().isEmpty()) {
					return AccessFlags.PUBLIC;
				}
			}
		}
		return -1;
	}

	private static int fixMethodVisibility(MethodNode mth) {
		AccessInfo accessFlags = mth.getAccessFlags();
		if (accessFlags.isPublic()) {
			return -1;
		}
		MethodOverrideAttr overrideAttr = mth.get(AType.METHOD_OVERRIDE);
		if (overrideAttr != null && !overrideAttr.getOverrideList().isEmpty()) {
			// visibility can't be weaker
			IMethodDetails parentMD = overrideAttr.getOverrideList().get(0);
			AccessInfo parentAccInfo = new AccessInfo(parentMD.getRawAccessFlags(), AccessInfo.AFType.METHOD);
			if (accessFlags.isVisibilityWeakerThan(parentAccInfo)) {
				return parentAccInfo.getVisibility().rawValue();
			}
		}
		if (mth.getUseIn().isEmpty()) {
			return -1;
		}

		ClassNode thisTopParentCls = mth.getParentClass().getTopParentClass();
		for (MethodNode useMth : mth.getUseIn()) {
			ClassNode useInTPCls = useMth.getParentClass().getTopParentClass();
			if (!useInTPCls.equals(thisTopParentCls)) {
				return AccessFlags.PUBLIC;
			}
		}
		return -1;
	}
}
