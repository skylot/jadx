package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.core.clsp.ClspClass;
import jadx.core.clsp.ClspMethod;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.MethodBridgeAttr;
import jadx.core.dex.attributes.nodes.MethodOverrideAttr;
import jadx.core.dex.attributes.nodes.RenameReasonAttr;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.IMethodDetails;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.rename.RenameVisitor;
import jadx.core.dex.visitors.typeinference.TypeCompare;
import jadx.core.dex.visitors.typeinference.TypeCompareEnum;
import jadx.core.dex.visitors.typeinference.TypeInferenceVisitor;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.exceptions.JadxRuntimeException;

@JadxVisitor(
		name = "OverrideMethodVisitor",
		desc = "Mark override methods and revert type erasure",
		runBefore = {
				TypeInferenceVisitor.class,
				RenameVisitor.class
		}
)
public class OverrideMethodVisitor extends AbstractVisitor {

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		SuperTypesData superData = collectSuperTypes(cls);
		if (superData != null) {
			for (MethodNode mth : cls.getMethods()) {
				processMth(mth, superData);
			}
		}
		return true;
	}

	private void processMth(MethodNode mth, SuperTypesData superData) {
		if (mth.isConstructor() || mth.getAccessFlags().isStatic() || mth.getAccessFlags().isPrivate()) {
			return;
		}
		MethodOverrideAttr attr = processOverrideMethods(mth, superData);
		if (attr != null) {
			if (attr.getBaseMethods().isEmpty()) {
				throw new JadxRuntimeException("No base methods for override attribute: " + attr.getOverrideList());
			}
			mth.addAttr(attr);
			IMethodDetails baseMth = Utils.getOne(attr.getBaseMethods());
			if (baseMth != null) {
				boolean updated = fixMethodReturnType(mth, baseMth, superData);
				updated |= fixMethodArgTypes(mth, baseMth, superData);
				if (updated) {
					// check if new signature cause method collisions
					checkMethodSignatureCollisions(mth, mth.root().getArgs().isRenameValid());
				}
			}
		}
	}

	private MethodOverrideAttr processOverrideMethods(MethodNode mth, SuperTypesData superData) {
		MethodOverrideAttr result = mth.get(AType.METHOD_OVERRIDE);
		if (result != null) {
			return result;
		}
		ClassNode cls = mth.getParentClass();
		String signature = mth.getMethodInfo().makeSignature(false);
		List<IMethodDetails> overrideList = new ArrayList<>();
		Set<IMethodDetails> baseMethods = new HashSet<>();
		for (ArgType superType : superData.getSuperTypes()) {
			ClassNode classNode = mth.root().resolveClass(superType);
			if (classNode != null) {
				MethodNode ovrdMth = searchOverriddenMethod(classNode, mth, signature);
				if (ovrdMth != null) {
					if (isMethodVisibleInCls(ovrdMth, cls)) {
						overrideList.add(ovrdMth);
						MethodOverrideAttr attr = ovrdMth.get(AType.METHOD_OVERRIDE);
						if (attr != null) {
							addBaseMethod(superData, overrideList, baseMethods, superType);
							return buildOverrideAttr(mth, overrideList, baseMethods, attr);
						}
					}
				}
			} else {
				ClspClass clsDetails = mth.root().getClsp().getClsDetails(superType);
				if (clsDetails != null) {
					Map<String, ClspMethod> methodsMap = clsDetails.getMethodsMap();
					for (Map.Entry<String, ClspMethod> entry : methodsMap.entrySet()) {
						String mthShortId = entry.getKey();
						// do not check full signature, classpath methods can be trusted
						// i.e. doesn't contain methods with same signature in one class
						if (mthShortId.startsWith(signature)) {
							overrideList.add(entry.getValue());
							break;
						}
					}
				}
			}
			addBaseMethod(superData, overrideList, baseMethods, superType);
		}
		return buildOverrideAttr(mth, overrideList, baseMethods, null);
	}

	private void addBaseMethod(SuperTypesData superData, List<IMethodDetails> overrideList, Set<IMethodDetails> baseMethods,
			ArgType superType) {
		if (superData.getEndTypes().contains(superType.getObject())) {
			IMethodDetails last = Utils.last(overrideList);
			if (last != null) {
				baseMethods.add(last);
			}
		}
	}

	@Nullable
	private MethodNode searchOverriddenMethod(ClassNode cls, MethodNode mth, String signature) {
		// search by exact full signature (with return value) to fight obfuscation (see test
		// 'TestOverrideWithSameName')
		String shortId = mth.getMethodInfo().getShortId();
		for (MethodNode supMth : cls.getMethods()) {
			if (supMth.getMethodInfo().getShortId().equals(shortId) && !supMth.getAccessFlags().isStatic()) {
				return supMth;
			}
		}
		// search by signature without return value and check if return value is wider type
		for (MethodNode supMth : cls.getMethods()) {
			if (supMth.getMethodInfo().getShortId().startsWith(signature) && !supMth.getAccessFlags().isStatic()) {
				TypeCompare typeCompare = cls.root().getTypeCompare();
				ArgType supRetType = supMth.getMethodInfo().getReturnType();
				ArgType mthRetType = mth.getMethodInfo().getReturnType();
				TypeCompareEnum res = typeCompare.compareTypes(supRetType, mthRetType);
				if (res.isWider()) {
					return supMth;
				}
				if (res == TypeCompareEnum.UNKNOWN || res == TypeCompareEnum.CONFLICT) {
					mth.addDebugComment("Possible override for method " + supMth.getMethodInfo().getFullId());
				}
			}
		}
		return null;
	}

	@Nullable
	private MethodOverrideAttr buildOverrideAttr(MethodNode mth, List<IMethodDetails> overrideList,
			Set<IMethodDetails> baseMethods, @Nullable MethodOverrideAttr attr) {
		if (overrideList.isEmpty() && attr == null) {
			return null;
		}
		if (attr == null) {
			// traced to base method
			List<IMethodDetails> cleanOverrideList = overrideList.stream().distinct().collect(Collectors.toList());
			return applyOverrideAttr(mth, cleanOverrideList, baseMethods, false);
		}
		// trace stopped at already processed method -> start merging
		List<IMethodDetails> mergedOverrideList = Utils.mergeLists(overrideList, attr.getOverrideList());
		List<IMethodDetails> cleanOverrideList = mergedOverrideList.stream().distinct().collect(Collectors.toList());
		Set<IMethodDetails> mergedBaseMethods = Utils.mergeSets(baseMethods, attr.getBaseMethods());
		return applyOverrideAttr(mth, cleanOverrideList, mergedBaseMethods, true);
	}

	private MethodOverrideAttr applyOverrideAttr(MethodNode mth, List<IMethodDetails> overrideList,
			Set<IMethodDetails> baseMethods, boolean update) {
		// don't rename method if override list contains not resolved method
		boolean dontRename = overrideList.stream().anyMatch(m -> !(m instanceof MethodNode));
		SortedSet<MethodNode> relatedMethods = null;
		List<MethodNode> mthNodes = getMethodNodes(mth, overrideList);
		if (update) {
			// merge related methods from all override attributes
			for (MethodNode mthNode : mthNodes) {
				MethodOverrideAttr ovrdAttr = mthNode.get(AType.METHOD_OVERRIDE);
				if (ovrdAttr != null) {
					// use one of already allocated sets
					relatedMethods = ovrdAttr.getRelatedMthNodes();
					break;
				}
			}
			if (relatedMethods != null) {
				relatedMethods.addAll(mthNodes);
			} else {
				relatedMethods = new TreeSet<>(mthNodes);
			}
			for (MethodNode mthNode : mthNodes) {
				MethodOverrideAttr ovrdAttr = mthNode.get(AType.METHOD_OVERRIDE);
				if (ovrdAttr != null) {
					SortedSet<MethodNode> set = ovrdAttr.getRelatedMthNodes();
					if (relatedMethods != set) {
						relatedMethods.addAll(set);
					}
				}
			}
		} else {
			relatedMethods = new TreeSet<>(mthNodes);
		}

		int depth = 0;
		for (MethodNode mthNode : mthNodes) {
			if (dontRename) {
				mthNode.add(AFlag.DONT_RENAME);
			}
			if (depth == 0) {
				// skip current (first) method
				depth = 1;
				continue;
			}
			if (update) {
				MethodOverrideAttr ovrdAttr = mthNode.get(AType.METHOD_OVERRIDE);
				if (ovrdAttr != null) {
					ovrdAttr.setRelatedMthNodes(relatedMethods);
					continue;
				}
			}
			mthNode.addAttr(new MethodOverrideAttr(Utils.listTail(overrideList, depth), relatedMethods, baseMethods));
			depth++;
		}
		return new MethodOverrideAttr(overrideList, relatedMethods, baseMethods);
	}

	@NotNull
	private List<MethodNode> getMethodNodes(MethodNode mth, List<IMethodDetails> overrideList) {
		List<MethodNode> list = new ArrayList<>(1 + overrideList.size());
		list.add(mth);
		for (IMethodDetails md : overrideList) {
			if (md instanceof MethodNode) {
				list.add((MethodNode) md);
			}
		}
		return list;
	}

	/**
	 * NOTE: Simplified version of method from ModVisitor.isFieldVisibleInMethod
	 */
	private boolean isMethodVisibleInCls(MethodNode superMth, ClassNode cls) {
		AccessInfo accessFlags = superMth.getAccessFlags();
		if (accessFlags.isPrivate()) {
			return false;
		}
		if (accessFlags.isPublic() || accessFlags.isProtected()) {
			return true;
		}
		// package-private
		return Objects.equals(superMth.getParentClass().getPackage(), cls.getPackage());
	}

	private static final class SuperTypesData {
		private final List<ArgType> superTypes;
		private final Set<String> endTypes;

		private SuperTypesData(List<ArgType> superTypes, Set<String> endTypes) {
			this.superTypes = superTypes;
			this.endTypes = endTypes;
		}

		public List<ArgType> getSuperTypes() {
			return superTypes;
		}

		public Set<String> getEndTypes() {
			return endTypes;
		}
	}

	@Nullable
	private SuperTypesData collectSuperTypes(ClassNode cls) {
		List<ArgType> superTypes = new ArrayList<>();
		Set<String> endTypes = new HashSet<>();
		collectSuperTypes(cls, superTypes, endTypes);
		if (superTypes.isEmpty()) {
			return null;
		}
		if (endTypes.isEmpty()) {
			throw new JadxRuntimeException("No end types in class hierarchy: " + cls);
		}
		return new SuperTypesData(superTypes, endTypes);
	}

	private void collectSuperTypes(ClassNode cls, List<ArgType> superTypes, Set<String> endTypes) {
		RootNode root = cls.root();
		int k = 0;
		ArgType superClass = cls.getSuperClass();
		if (superClass != null) {
			k += addSuperType(root, superTypes, endTypes, superClass);
		}
		for (ArgType iface : cls.getInterfaces()) {
			k += addSuperType(root, superTypes, endTypes, iface);
		}
		if (k == 0) {
			endTypes.add(cls.getType().getObject());
		}
	}

	private int addSuperType(RootNode root, List<ArgType> superTypesMap, Set<String> endTypes, ArgType superType) {
		if (Objects.equals(superType, ArgType.OBJECT)) {
			return 0;
		}
		superTypesMap.add(superType);
		ClassNode classNode = root.resolveClass(superType);
		if (classNode != null) {
			collectSuperTypes(classNode, superTypesMap, endTypes);
			return 1;
		}
		ClspClass clsDetails = root.getClsp().getClsDetails(superType);
		if (clsDetails != null) {
			int k = 0;
			for (ArgType parentType : clsDetails.getParents()) {
				k += addSuperType(root, superTypesMap, endTypes, parentType);
			}
			if (k == 0) {
				endTypes.add(superType.getObject());
			}
			return 1;
		}
		// no info found => treat as hierarchy end
		endTypes.add(superType.getObject());
		return 1;
	}

	private boolean fixMethodReturnType(MethodNode mth, IMethodDetails baseMth, SuperTypesData superData) {
		ArgType returnType = mth.getReturnType();
		if (returnType == ArgType.VOID) {
			return false;
		}
		boolean updated = updateReturnType(mth, baseMth, superData);
		if (updated) {
			mth.addDebugComment("Return type fixed from '" + returnType + "' to match base method");
		}
		return updated;
	}

	private boolean updateReturnType(MethodNode mth, IMethodDetails baseMth, SuperTypesData superData) {
		ArgType baseReturnType = baseMth.getReturnType();
		if (mth.getReturnType().equals(baseReturnType)) {
			return false;
		}
		if (!baseReturnType.containsTypeVariable()) {
			return false;
		}
		TypeCompare typeCompare = mth.root().getTypeUpdate().getTypeCompare();
		ArgType baseCls = baseMth.getMethodInfo().getDeclClass().getType();
		for (ArgType superType : superData.getSuperTypes()) {
			TypeCompareEnum compareResult = typeCompare.compareTypes(superType, baseCls);
			if (compareResult == TypeCompareEnum.NARROW_BY_GENERIC) {
				ArgType targetRetType = mth.root().getTypeUtils().replaceClassGenerics(superType, baseReturnType);
				if (targetRetType != null
						&& !targetRetType.containsTypeVariable()
						&& !targetRetType.equals(mth.getReturnType())) {
					mth.updateReturnType(targetRetType);
					return true;
				}
			}
		}
		return false;
	}

	private boolean fixMethodArgTypes(MethodNode mth, IMethodDetails baseMth, SuperTypesData superData) {
		List<ArgType> mthArgTypes = mth.getArgTypes();
		List<ArgType> baseArgTypes = baseMth.getArgTypes();
		if (mthArgTypes.equals(baseArgTypes)) {
			return false;
		}
		int argCount = mthArgTypes.size();
		if (argCount != baseArgTypes.size()) {
			return false;
		}
		boolean changed = false;
		List<ArgType> newArgTypes = new ArrayList<>(argCount);
		for (int argNum = 0; argNum < argCount; argNum++) {
			ArgType newType = updateArgType(mth, baseMth, superData, argNum);
			if (newType != null) {
				changed = true;
				newArgTypes.add(newType);
			} else {
				newArgTypes.add(mthArgTypes.get(argNum));
			}
		}
		if (changed) {
			mth.updateArgTypes(newArgTypes, "Method arguments types fixed to match base method");
		}
		return changed;
	}

	private ArgType updateArgType(MethodNode mth, IMethodDetails baseMth, SuperTypesData superData, int argNum) {
		ArgType arg = mth.getArgTypes().get(argNum);
		ArgType baseArg = baseMth.getArgTypes().get(argNum);
		if (arg.equals(baseArg)) {
			return null;
		}
		if (!baseArg.containsTypeVariable()) {
			return null;
		}
		TypeCompare typeCompare = mth.root().getTypeUpdate().getTypeCompare();
		ArgType baseCls = baseMth.getMethodInfo().getDeclClass().getType();
		for (ArgType superType : superData.getSuperTypes()) {
			TypeCompareEnum compareResult = typeCompare.compareTypes(superType, baseCls);
			if (compareResult == TypeCompareEnum.NARROW_BY_GENERIC) {
				ArgType targetArgType = mth.root().getTypeUtils().replaceClassGenerics(superType, baseArg);
				if (targetArgType != null
						&& !targetArgType.containsTypeVariable()
						&& !targetArgType.equals(arg)) {
					return targetArgType;
				}
			}
		}
		return null;
	}

	private void checkMethodSignatureCollisions(MethodNode mth, boolean rename) {
		String mthName = mth.getMethodInfo().getAlias();
		String newSignature = MethodInfo.makeShortId(mthName, mth.getArgTypes(), null);
		for (MethodNode otherMth : mth.getParentClass().getMethods()) {
			String otherMthName = otherMth.getAlias();
			if (otherMthName.equals(mthName) && otherMth != mth) {
				String otherSignature = otherMth.getMethodInfo().makeSignature(true, false);
				if (otherSignature.equals(newSignature)) {
					if (rename) {
						if (otherMth.contains(AFlag.DONT_RENAME) || otherMth.contains(AType.METHOD_OVERRIDE)) {
							otherMth.addWarnComment("Can't rename method to resolve collision");
						} else {
							otherMth.getMethodInfo().setAlias(makeNewAlias(otherMth));
							otherMth.addAttr(new RenameReasonAttr("avoid collision after fix types in other method"));
						}
					}
					otherMth.addAttr(new MethodBridgeAttr(mth));
					return;
				}
			}
		}
	}

	// TODO: at this point deobfuscator is not available and map file already saved
	private static String makeNewAlias(MethodNode mth) {
		ClassNode cls = mth.getParentClass();
		String baseName = mth.getAlias();
		int k = 2;
		while (true) {
			String alias = baseName + k;
			MethodNode methodNode = cls.searchMethodByShortName(alias);
			if (methodNode == null) {
				return alias;
			}
			k++;
		}
	}

	@Override
	public String getName() {
		return "OverrideMethodVisitor";
	}
}
