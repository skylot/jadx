package jadx.core.dex.visitors.fixaccessmodifiers;

import java.util.Set;
import java.util.stream.Collectors;

import jadx.api.plugins.input.data.AccessFlags;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.MethodInlineAttr;
import jadx.core.dex.attributes.nodes.MethodOverrideAttr;
import jadx.core.dex.attributes.nodes.NotificationAttrNode;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.IMethodDetails;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.JadxVisitor;
import jadx.core.dex.visitors.ModVisitor;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "FixAccessModifiers",
		desc = "Change class and method access modifiers if needed",
		runAfter = ModVisitor.class
)
public class FixAccessModifiers extends AbstractVisitor {

	private VisibilityUtils visibilityUtils;

	private boolean respectAccessModifiers;

	@Override
	public void init(RootNode root) {
		this.visibilityUtils = new VisibilityUtils(root);
		this.respectAccessModifiers = root.getArgs().isRespectBytecodeAccModifiers();
	}

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		if (respectAccessModifiers) {
			return true;
		}

		fixClassVisibility(cls);
		return true;
	}

	@Override
	public void visit(MethodNode mth) {
		if (respectAccessModifiers || mth.contains(AFlag.DONT_GENERATE)) {
			return;
		}

		fixMethodVisibility(mth);
	}

	private void fixClassVisibility(ClassNode cls) {
		AccessInfo accessFlags = cls.getAccessFlags();
		if (cls.isTopClass() && accessFlags.isPublic()) {
			return;
		}

		if (cls.isTopClass() && (accessFlags.isPrivate() || accessFlags.isProtected())) {
			changeVisibility(cls, AccessFlags.PUBLIC);
			return;
		}

		for (ClassNode useCls : cls.getUseIn()) {
			visibilityUtils.checkVisibility(cls, useCls, (node, visFlag) -> {
				changeVisibility((NotificationAttrNode) node, visFlag);
			});
		}

		for (MethodNode useMth : cls.getUseInMth()) {
			MethodInlineAttr inlineAttr = useMth.get(AType.METHOD_INLINE);
			boolean isInline = inlineAttr != null && !inlineAttr.notNeeded();
			boolean isCandidateForInline = useMth.contains(AFlag.METHOD_CANDIDATE_FOR_INLINE);

			if (isInline || isCandidateForInline) {
				Set<ClassNode> usedInClss = useMth.getUseIn().stream()
						.map(MethodNode::getParentClass)
						.collect(Collectors.toSet());

				for (ClassNode useCls : usedInClss) {
					visibilityUtils.checkVisibility(cls, useCls, (node, visFlag) -> {
						changeVisibility((NotificationAttrNode) node, visFlag);
					});
				}
			}
		}
	}

	private void fixMethodVisibility(MethodNode mth) {
		AccessInfo accessFlags = mth.getAccessFlags();
		MethodOverrideAttr overrideAttr = mth.get(AType.METHOD_OVERRIDE);
		if (overrideAttr != null && !overrideAttr.getOverrideList().isEmpty()) {
			// visibility can't be weaker
			IMethodDetails parentMD = overrideAttr.getOverrideList().get(0);
			AccessInfo parentAccInfo = new AccessInfo(parentMD.getRawAccessFlags(), AccessInfo.AFType.METHOD);
			if (accessFlags.isVisibilityWeakerThan(parentAccInfo)) {
				changeVisibility(mth, parentAccInfo.getVisibility().rawValue());
			}
		}

		for (MethodNode useMth : mth.getUseIn()) {
			visibilityUtils.checkVisibility(mth, useMth, (node, visFlag) -> {
				changeVisibility((NotificationAttrNode) node, visFlag);
			});
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
}
