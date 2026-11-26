package jadx.gui.ui.dialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import jadx.api.JadxDecompiler;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.MethodBridgeAttr;
import jadx.core.dex.attributes.nodes.MethodOverrideAttr;
import jadx.core.dex.attributes.nodes.MethodReplaceAttr;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.nodes.IMethodDetails;
import jadx.core.dex.nodes.MethodNode;

/**
 * Pure helper for finding concrete implementing or overriding methods of a target method
 * using {@link MethodOverrideAttr} metadata populated by {@code OverrideMethodVisitor}.
 */
public final class MethodImplementationsFinder {

	private MethodImplementationsFinder() {
	}

	public static boolean hasImplementations(@Nullable JavaMethod target) {
		return target != null && hasImplementations(target.getMethodNode());
	}

	public static boolean hasImplementations(@Nullable MethodNode target) {
		MethodNode effectiveTarget = resolveTarget(target);
		if (!isOverridable(effectiveTarget)) {
			return false;
		}
		MethodNode targetBridge = findTargetBridge(effectiveTarget);
		for (MethodNode candidate : collectRelatedCandidates(effectiveTarget, targetBridge)) {
			if (resolveImplementation(candidate, effectiveTarget, targetBridge) != null) {
				return true;
			}
		}
		return false;
	}

	public static List<JavaMethod> collectImplementations(@Nullable JavaMethod target) {
		if (target == null) {
			return Collections.emptyList();
		}
		MethodNode targetNode = target.getMethodNode();
		List<MethodNode> implNodes = collectImplementations(targetNode);
		if (implNodes.isEmpty()) {
			return Collections.emptyList();
		}
		JadxDecompiler decompiler = targetNode.root().getDecompiler();
		if (decompiler == null) {
			return Collections.emptyList();
		}
		List<JavaMethod> implementations = new ArrayList<>(implNodes.size());
		for (MethodNode implNode : implNodes) {
			JavaNode javaNode = decompiler.getJavaNodeByRef(implNode);
			if (javaNode instanceof JavaMethod) {
				implementations.add((JavaMethod) javaNode);
			}
		}
		return implementations;
	}

	public static List<MethodNode> collectImplementations(@Nullable MethodNode target) {
		MethodNode effectiveTarget = resolveTarget(target);
		if (!isOverridable(effectiveTarget)) {
			return Collections.emptyList();
		}
		MethodNode targetBridge = findTargetBridge(effectiveTarget);
		Set<MethodNode> implementations = new LinkedHashSet<>();
		for (MethodNode candidate : collectRelatedCandidates(effectiveTarget, targetBridge)) {
			MethodNode resolved = resolveImplementation(candidate, effectiveTarget, targetBridge);
			if (resolved != null) {
				implementations.add(resolved);
			}
		}
		return new ArrayList<>(implementations);
	}

	/**
	 * Resolve the {@link MethodNode} holding the code definition position for jumping.
	 * When {@code ClassModifier} merges a bridge method into a concrete method, {@code MethodGen}
	 * attaches {@code NodeDeclareRef} to {@link MethodReplaceAttr#getReplaceMth()} rather than the
	 * concrete {@code MethodNode}.
	 */
	public static @Nullable MethodNode resolveJumpMethod(@Nullable MethodNode mth) {
		if (mth == null) {
			return null;
		}
		if (mth.getDefPosition() > 0) {
			return mth;
		}
		MethodReplaceAttr replaceAttr = mth.get(AType.METHOD_REPLACE);
		if (replaceAttr != null && replaceAttr.getReplaceMth().getDefPosition() > 0) {
			return replaceAttr.getReplaceMth();
		}
		return mth;
	}

	public static int resolveDefPosition(@Nullable MethodNode mth) {
		MethodNode jumpMth = resolveJumpMethod(mth);
		return jumpMth != null ? jumpMth.getDefPosition() : 0;
	}

	private static @Nullable MethodNode resolveTarget(@Nullable MethodNode target) {
		if (target == null) {
			return null;
		}
		if (isBridgeOrHidden(target)) {
			MethodNode sibling = findConcreteSibling(target, null);
			if (sibling != null) {
				return sibling;
			}
		}
		return target;
	}

	private static @Nullable MethodNode findTargetBridge(MethodNode target) {
		MethodReplaceAttr replaceAttr = target.get(AType.METHOD_REPLACE);
		if (replaceAttr != null) {
			return replaceAttr.getReplaceMth();
		}
		MethodBridgeAttr bridgeAttr = target.get(AType.BRIDGED_BY);
		return bridgeAttr != null ? bridgeAttr.getBridgeMth() : null;
	}

	private static Set<MethodNode> collectRelatedCandidates(MethodNode target, @Nullable MethodNode targetBridge) {
		MethodOverrideAttr targetAttr = target.get(AType.METHOD_OVERRIDE);
		MethodOverrideAttr bridgeAttr = targetBridge != null ? targetBridge.get(AType.METHOD_OVERRIDE) : null;
		if (targetAttr == null && bridgeAttr == null) {
			return Collections.emptySet();
		}
		if (bridgeAttr == null) {
			return targetAttr.getRelatedMthNodes();
		}
		if (targetAttr == null) {
			return bridgeAttr.getRelatedMthNodes();
		}
		if (targetAttr.getRelatedMthNodes() == bridgeAttr.getRelatedMthNodes()) {
			return targetAttr.getRelatedMthNodes();
		}
		Set<MethodNode> combined = new LinkedHashSet<>(targetAttr.getRelatedMthNodes());
		combined.addAll(bridgeAttr.getRelatedMthNodes());
		return combined;
	}

	private static @Nullable MethodNode resolveImplementation(
			MethodNode candidate, MethodNode target, @Nullable MethodNode targetBridge) {
		if (!isDirectOverride(candidate, target, targetBridge)) {
			return null;
		}
		if (isBridgeOrHidden(candidate)) {
			MethodNode sibling = findConcreteSibling(candidate, target);
			if (sibling != null) {
				return sibling;
			}
			if (candidate.contains(AFlag.DONT_GENERATE)) {
				return null;
			}
			if (candidate.getAccessFlags().isBridge()
					&& hasConcreteOverrideSibling(candidate, target, targetBridge)) {
				return null;
			}
		}
		return candidate;
	}

	private static boolean isDirectOverride(MethodNode candidate, MethodNode target, @Nullable MethodNode targetBridge) {
		if (candidate == target || candidate.equals(target)) {
			return false;
		}
		if (targetBridge != null && (candidate == targetBridge || candidate.equals(targetBridge))) {
			return false;
		}
		if (candidate.getAccessFlags().isAbstract()) {
			return false;
		}
		MethodOverrideAttr candidateAttr = candidate.get(AType.METHOD_OVERRIDE);
		if (candidateAttr == null) {
			return false;
		}
		MethodInfo targetInfo = target.getMethodInfo();
		MethodInfo bridgeInfo = targetBridge != null ? targetBridge.getMethodInfo() : null;
		for (IMethodDetails overridden : candidateAttr.getOverrideList()) {
			MethodInfo overriddenInfo = overridden.getMethodInfo();
			if (overriddenInfo.equals(targetInfo) || (bridgeInfo != null && overriddenInfo.equals(bridgeInfo))) {
				return true;
			}
		}
		return false;
	}

	private static boolean isBridgeOrHidden(MethodNode mth) {
		AccessInfo accessFlags = mth.getAccessFlags();
		return accessFlags.isBridge() || accessFlags.isSynthetic() || mth.contains(AFlag.DONT_GENERATE);
	}

	private static boolean hasConcreteOverrideSibling(
			MethodNode bridgeCandidate, MethodNode target, @Nullable MethodNode targetBridge) {
		String name = bridgeCandidate.getName();
		int argCount = bridgeCandidate.getArgTypes().size();
		for (MethodNode sibling : bridgeCandidate.getParentClass().getMethods()) {
			if (sibling == bridgeCandidate || sibling.equals(bridgeCandidate) || isBridgeOrHidden(sibling)) {
				continue;
			}
			if (sibling.getName().equals(name)
					&& sibling.getArgTypes().size() == argCount
					&& isDirectOverride(sibling, target, targetBridge)) {
				return true;
			}
		}
		return false;
	}

	private static @Nullable MethodNode findConcreteSibling(MethodNode candidate, @Nullable MethodNode excludedTarget) {
		for (MethodNode sibling : candidate.getParentClass().getMethods()) {
			if (sibling == candidate || sibling.equals(candidate) || sibling.equals(excludedTarget)) {
				continue;
			}
			if (sibling.getAccessFlags().isAbstract() || sibling.contains(AFlag.DONT_GENERATE)) {
				continue;
			}
			MethodReplaceAttr replaceAttr = sibling.get(AType.METHOD_REPLACE);
			if (replaceAttr != null && candidate.equals(replaceAttr.getReplaceMth())) {
				return sibling;
			}
			MethodBridgeAttr bridgeAttr = sibling.get(AType.BRIDGED_BY);
			if (bridgeAttr != null && candidate.equals(bridgeAttr.getBridgeMth())) {
				return sibling;
			}
		}
		return null;
	}

	private static boolean isOverridable(@Nullable MethodNode target) {
		return target != null
				&& !target.isConstructor()
				&& !target.getAccessFlags().isStatic()
				&& !target.getAccessFlags().isPrivate();
	}
}
