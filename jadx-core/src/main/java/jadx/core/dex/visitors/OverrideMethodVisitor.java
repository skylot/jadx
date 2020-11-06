package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jadx.core.clsp.ClspClass;
import jadx.core.clsp.ClspMethod;
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
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "OverrideMethodVisitor",
		desc = "Mark override methods and revert type erasure",
		runBefore = {
				TypeInferenceVisitor.class
		}
)
public class OverrideMethodVisitor extends AbstractVisitor {

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		List<ArgType> superTypes = collectSuperTypes(cls);
		for (MethodNode mth : cls.getMethods()) {
			processMth(cls, superTypes, mth);
		}
		return true;
	}

	private void processMth(ClassNode cls, List<ArgType> superTypes, MethodNode mth) {
		if (mth.isConstructor() || mth.getAccessFlags().isStatic()) {
			return;
		}
		mth.remove(AType.METHOD_OVERRIDE);
		String signature = mth.getMethodInfo().makeSignature(false);
		List<IMethodDetails> overrideList = collectOverrideMethods(cls, superTypes, signature);
		if (!overrideList.isEmpty()) {
			mth.addAttr(new MethodOverrideAttr(overrideList));
			fixMethodReturnType(mth, overrideList, superTypes);
			fixMethodArgTypes(mth, overrideList, superTypes);
		}
	}

	private List<IMethodDetails> collectOverrideMethods(ClassNode cls, List<ArgType> superTypes, String signature) {
		List<IMethodDetails> overrideList = new ArrayList<>();
		for (ArgType superType : superTypes) {
			ClassNode classNode = cls.root().resolveClass(superType);
			if (classNode != null) {
				for (MethodNode mth : classNode.getMethods()) {
					if (!mth.getAccessFlags().isStatic()
							&& isMethodVisibleInCls(mth, cls)) {
						String mthShortId = mth.getMethodInfo().getShortId();
						if (mthShortId.startsWith(signature)) {
							overrideList.add(mth);
						}
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
		return overrideList;
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
		Map<String, ArgType> superTypes = new HashMap<>();
		collectSuperTypes(cls, superTypes);
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

	private void fixMethodReturnType(MethodNode mth, List<IMethodDetails> overrideList, List<ArgType> superTypes) {
		ArgType returnType = mth.getReturnType();
		if (returnType == ArgType.VOID) {
			return;
		}
		int updateCount = 0;
		for (IMethodDetails baseMth : overrideList) {
			if (updateReturnType(mth, baseMth, superTypes)) {
				updateCount++;
			}
		}
		if (updateCount == 0) {
			return;
		}
		if (updateCount == 1) {
			mth.addComment("Return type fixed from '" + returnType + "' to match base method");
		} else {
			mth.addWarnComment("Due to multiple override return type can be incorrect, original value: " + returnType);
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

	private void fixMethodArgTypes(MethodNode mth, List<IMethodDetails> overrideList, List<ArgType> superTypes) {
		for (IMethodDetails baseMth : overrideList) {
			updateArgTypes(mth, baseMth, superTypes);
		}
	}

	private void updateArgTypes(MethodNode mth, IMethodDetails baseMth, List<ArgType> superTypes) {
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
