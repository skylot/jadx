package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.core.clsp.ClspClass;
import jadx.core.clsp.ClspMethod;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.MethodOverrideAttr;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.IMethodDetails;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.typeinference.TypeCompare;
import jadx.core.dex.visitors.typeinference.TypeCompareEnum;
import jadx.core.dex.visitors.typeinference.TypeInferenceVisitor;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxException;

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
		processCls(cls);
		return true;
	}

	private void processCls(ClassNode cls) {
		List<ArgType> superTypes = collectSuperTypes(cls);
		if (!superTypes.isEmpty()) {
			for (MethodNode mth : cls.getMethods()) {
				processMth(cls, superTypes, mth);
			}
		}
	}

	private void processMth(ClassNode cls, List<ArgType> superTypes, MethodNode mth) {
		if (mth.isConstructor() || mth.getAccessFlags().isStatic() || mth.getAccessFlags().isPrivate()) {
			return;
		}
		MethodOverrideAttr attr = processOverrideMethods(cls, mth, superTypes);
		if (attr != null) {
			mth.addAttr(attr);
			IMethodDetails baseMth = Utils.last(attr.getOverrideList());
			if (baseMth != null) {
				fixMethodReturnType(mth, baseMth, superTypes);
				fixMethodArgTypes(mth, baseMth, superTypes);
			}
		}
	}

	private MethodOverrideAttr processOverrideMethods(ClassNode cls, MethodNode mth, List<ArgType> superTypes) {
		MethodOverrideAttr result = mth.get(AType.METHOD_OVERRIDE);
		if (result != null) {
			return result;
		}
		String signature = mth.getMethodInfo().makeSignature(false);
		List<IMethodDetails> overrideList = new ArrayList<>();
		for (ArgType superType : superTypes) {
			ClassNode classNode = cls.root().resolveClass(superType);
			if (classNode != null) {
				MethodNode ovrdMth = searchOverriddenMethod(classNode, signature);
				if (ovrdMth != null && isMethodVisibleInCls(ovrdMth, cls)) {
					overrideList.add(ovrdMth);
					MethodOverrideAttr attr = ovrdMth.get(AType.METHOD_OVERRIDE);
					if (attr != null) {
						return buildOverrideAttr(mth, overrideList, attr);
					}
				}
			} else {
				ClspClass clsDetails = cls.root().getClsp().getClsDetails(superType);
				if (clsDetails != null) {
					Map<String, ClspMethod> methodsMap = clsDetails.getMethodsMap();
					for (Map.Entry<String, ClspMethod> entry : methodsMap.entrySet()) {
						String mthShortId = entry.getKey();
						if (mthShortId.startsWith(signature)) {
							overrideList.add(entry.getValue());
						}
					}
				}
			}
		}
		return buildOverrideAttr(mth, overrideList, null);
	}

	@Nullable
	private MethodNode searchOverriddenMethod(ClassNode cls, String signature) {
		for (MethodNode supMth : cls.getMethods()) {
			if (!supMth.getAccessFlags().isStatic() && supMth.getMethodInfo().getShortId().startsWith(signature)) {
				return supMth;
			}
		}
		return null;
	}

	@Nullable
	private MethodOverrideAttr buildOverrideAttr(MethodNode mth, List<IMethodDetails> overrideList,
			@Nullable MethodOverrideAttr attr) {
		if (overrideList.isEmpty() && attr == null) {
			return null;
		}
		if (attr == null) {
			// traced to base method
			List<IMethodDetails> cleanOverrideList = overrideList.stream().distinct().collect(Collectors.toList());
			return applyOverrideAttr(mth, cleanOverrideList, false);
		}
		// trace stopped at already processed method -> start merging
		List<IMethodDetails> mergedOverrideList = Utils.mergeLists(overrideList, attr.getOverrideList());
		List<IMethodDetails> cleanOverrideList = mergedOverrideList.stream().distinct().collect(Collectors.toList());
		return applyOverrideAttr(mth, cleanOverrideList, true);
	}

	private MethodOverrideAttr applyOverrideAttr(MethodNode mth, List<IMethodDetails> overrideList, boolean update) {
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
			mthNode.addAttr(new MethodOverrideAttr(Utils.listTail(overrideList, depth), relatedMethods));
			depth++;
		}
		return new MethodOverrideAttr(overrideList, relatedMethods);
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

	private List<ArgType> collectSuperTypes(ClassNode cls) {
		Map<String, ArgType> superTypes = new LinkedHashMap<>();
		collectSuperTypes(cls, superTypes);
		if (superTypes.isEmpty()) {
			return Collections.emptyList();
		}
		return new ArrayList<>(superTypes.values());
	}

	private void collectSuperTypes(ClassNode cls, Map<String, ArgType> superTypes) {
		RootNode root = cls.root();
		ArgType superClass = cls.getSuperClass();
		if (superClass != null && !Objects.equals(superClass, ArgType.OBJECT)) {
			addSuperType(root, superTypes, superClass);
		}
		for (ArgType iface : cls.getInterfaces()) {
			addSuperType(root, superTypes, iface);
		}
	}

	private void addSuperType(RootNode root, Map<String, ArgType> superTypesMap, ArgType superType) {
		superTypesMap.put(superType.getObject(), superType);
		ClassNode classNode = root.resolveClass(superType);
		if (classNode == null) {
			for (String superCls : root.getClsp().getSuperTypes(superType.getObject())) {
				ArgType type = ArgType.object(superCls);
				superTypesMap.put(type.getObject(), type);
			}
		} else {
			collectSuperTypes(classNode, superTypesMap);
		}
	}

	private void fixMethodReturnType(MethodNode mth, IMethodDetails baseMth, List<ArgType> superTypes) {
		ArgType returnType = mth.getReturnType();
		if (returnType == ArgType.VOID) {
			return;
		}
		if (updateReturnType(mth, baseMth, superTypes)) {
			mth.addComment("Return type fixed from '" + returnType + "' to match base method");
		}
	}

	private boolean updateReturnType(MethodNode mth, IMethodDetails baseMth, List<ArgType> superTypes) {
		ArgType baseReturnType = baseMth.getReturnType();
		if (mth.getReturnType().equals(baseReturnType)) {
			return false;
		}
		if (!baseReturnType.containsTypeVariable()) {
			return false;
		}
		TypeCompare typeCompare = mth.root().getTypeUpdate().getTypeCompare();
		ArgType baseCls = baseMth.getMethodInfo().getDeclClass().getType();
		for (ArgType superType : superTypes) {
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

	private void fixMethodArgTypes(MethodNode mth, IMethodDetails baseMth, List<ArgType> superTypes) {
		List<ArgType> mthArgTypes = mth.getArgTypes();
		List<ArgType> baseArgTypes = baseMth.getArgTypes();
		if (mthArgTypes.equals(baseArgTypes)) {
			return;
		}
		int argCount = mthArgTypes.size();
		if (argCount != baseArgTypes.size()) {
			return;
		}
		boolean changed = false;
		List<ArgType> newArgTypes = new ArrayList<>(argCount);
		for (int argNum = 0; argNum < argCount; argNum++) {
			ArgType newType = updateArgType(mth, baseMth, superTypes, argNum);
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
	}

	private ArgType updateArgType(MethodNode mth, IMethodDetails baseMth, List<ArgType> superTypes, int argNum) {
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
		for (ArgType superType : superTypes) {
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
}
