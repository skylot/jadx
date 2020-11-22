package jadx.core.dex.nodes.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import jadx.core.clsp.ClspClass;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.MethodTypeVarsAttr;
import jadx.core.dex.attributes.nodes.NotificationAttrNode;
import jadx.core.dex.instructions.BaseInvokeNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.IMethodDetails;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.Utils;

import static jadx.core.utils.Utils.isEmpty;
import static jadx.core.utils.Utils.notEmpty;

public class TypeUtils {
	private final RootNode root;

	public TypeUtils(RootNode rootNode) {
		this.root = rootNode;
	}

	public List<ArgType> getClassGenerics(ArgType type) {
		ClassNode classNode = root.resolveClass(type);
		if (classNode != null) {
			return classNode.getGenericTypeParameters();
		}
		ClspClass clsDetails = root.getClsp().getClsDetails(type);
		if (clsDetails == null || clsDetails.getTypeParameters().isEmpty()) {
			return Collections.emptyList();
		}
		List<ArgType> generics = clsDetails.getTypeParameters();
		return generics == null ? Collections.emptyList() : generics;
	}

	public ArgType expandTypeVariables(ClassNode cls, ArgType type) {
		if (type.containsTypeVariable()) {
			expandTypeVar(cls, type, cls.getGenericTypeParameters());
		}
		return type;
	}

	public ArgType expandTypeVariables(MethodNode mth, ArgType type) {
		if (type.containsTypeVariable()) {
			expandTypeVar(mth, type, getKnownTypeVarsAtMethod(mth));
		}
		return type;
	}

	private void expandTypeVar(NotificationAttrNode node, ArgType type, Collection<ArgType> typeVars) {
		boolean allExtendsEmpty = true;
		for (ArgType argType : typeVars) {
			if (notEmpty(argType.getExtendTypes())) {
				allExtendsEmpty = false;
				break;
			}
		}
		if (allExtendsEmpty) {
			return;
		}
		type.visitTypes(t -> {
			if (t.isGenericType()) {
				String typeVarName = t.getObject();
				for (ArgType typeVar : typeVars) {
					if (typeVar.getObject().equals(typeVarName)) {
						t.setExtendTypes(typeVar.getExtendTypes());
						return null;
					}
				}
				node.addWarnComment("Unknown type variable: " + typeVarName + " in type: " + type);
			}
			return null;
		});
	}

	public Set<ArgType> getKnownTypeVarsAtMethod(MethodNode mth) {
		MethodTypeVarsAttr typeVarsAttr = mth.get(AType.METHOD_TYPE_VARS);
		if (typeVarsAttr != null) {
			return typeVarsAttr.getTypeVars();
		}
		Set<ArgType> typeVars = collectKnownTypeVarsAtMethod(mth);
		MethodTypeVarsAttr varsAttr = MethodTypeVarsAttr.build(typeVars);
		mth.addAttr(varsAttr);
		return varsAttr.getTypeVars();
	}

	private static Set<ArgType> collectKnownTypeVarsAtMethod(MethodNode mth) {
		ClassNode declCls = mth.getParentClass();
		Set<ArgType> typeVars = new HashSet<>(declCls.getGenericTypeParameters());
		declCls.visitParentClasses(parent -> typeVars.addAll(parent.getGenericTypeParameters()));

		typeVars.addAll(mth.getTypeParameters());
		return typeVars.isEmpty() ? Collections.emptySet() : typeVars;
	}

	/**
	 * Replace generic types in {@code typeWithGeneric} using instance types
	 * <br>
	 * Example:
	 * <ul>
	 * <li>{@code instanceType: Set<String>}
	 * <li>{@code typeWithGeneric: Iterator<E>}
	 * <li>{@code return: Iterator<String>}
	 * </ul>
	 */
	@Nullable
	public ArgType replaceClassGenerics(ArgType instanceType, ArgType typeWithGeneric) {
		if (typeWithGeneric == null) {
			return null;
		}
		Map<ArgType, ArgType> replaceMap = getTypeVariablesMapping(instanceType);
		if (replaceMap.isEmpty()) {
			return null;
		}
		return replaceTypeVariablesUsingMap(typeWithGeneric, replaceMap);
	}

	public Map<ArgType, ArgType> getTypeVariablesMapping(ArgType clsType) {
		if (!clsType.isGeneric()) {
			return Collections.emptyMap();
		}

		List<ArgType> typeParameters = root.getTypeUtils().getClassGenerics(clsType);
		if (typeParameters.isEmpty()) {
			return Collections.emptyMap();
		}
		List<ArgType> actualTypes = clsType.getGenericTypes();
		if (isEmpty(actualTypes)) {
			return Collections.emptyMap();
		}
		int genericParamsCount = actualTypes.size();
		if (genericParamsCount != typeParameters.size()) {
			return Collections.emptyMap();
		}
		Map<ArgType, ArgType> replaceMap = new HashMap<>(genericParamsCount);
		for (int i = 0; i < genericParamsCount; i++) {
			ArgType actualType = actualTypes.get(i);
			ArgType typeVar = typeParameters.get(i);
			replaceMap.put(typeVar, actualType);
		}
		return replaceMap;
	}

	public Map<ArgType, ArgType> getTypeVarMappingForInvoke(BaseInvokeNode invokeInsn) {
		IMethodDetails mthDetails = root.getMethodUtils().getMethodDetails(invokeInsn);
		if (mthDetails == null) {
			return Collections.emptyMap();
		}
		Map<ArgType, ArgType> map = new HashMap<>(1 + invokeInsn.getArgsCount());
		addTypeVarMapping(map, mthDetails.getReturnType(), invokeInsn.getResult());
		int argCount = Math.min(mthDetails.getArgTypes().size(), invokeInsn.getArgsCount());
		for (int i = 0; i < argCount; i++) {
			addTypeVarMapping(map, mthDetails.getArgTypes().get(i), invokeInsn.getArg(i));
		}
		return map;
	}

	private static void addTypeVarMapping(Map<ArgType, ArgType> map, ArgType typeVar, InsnArg arg) {
		if (arg == null || typeVar == null || !typeVar.isTypeKnown()) {
			return;
		}
		if (typeVar.isGenericType()) {
			map.put(typeVar, arg.getType());
		}
		// TODO: resolve inner type vars: 'List<T> -> List<String>' to 'T -> String'
	}

	@Nullable
	public ArgType replaceMethodGenerics(BaseInvokeNode invokeInsn, IMethodDetails details, ArgType typeWithGeneric) {
		if (typeWithGeneric == null) {
			return null;
		}
		List<ArgType> methodArgTypes = details.getArgTypes();
		if (methodArgTypes.isEmpty()) {
			return null;
		}
		int firstArgOffset = invokeInsn.getFirstArgOffset();
		int argsCount = methodArgTypes.size();
		for (int i = 0; i < argsCount; i++) {
			ArgType methodArgType = methodArgTypes.get(i);
			InsnArg insnArg = invokeInsn.getArg(i + firstArgOffset);
			ArgType insnType = insnArg.getType();
			if (methodArgType.equals(typeWithGeneric)) {
				return insnType;
			}
		}
		// TODO build complete map for type variables
		return null;
	}

	@Nullable
	public ArgType replaceTypeVariablesUsingMap(ArgType replaceType, Map<ArgType, ArgType> replaceMap) {
		if (replaceMap.isEmpty()) {
			return null;
		}
		if (replaceType.isGenericType()) {
			return replaceMap.get(replaceType);
		}
		if (replaceType.isArray()) {
			ArgType replaced = replaceTypeVariablesUsingMap(replaceType.getArrayElement(), replaceMap);
			if (replaced == null) {
				return null;
			}
			return ArgType.array(replaced);
		}

		ArgType wildcardType = replaceType.getWildcardType();
		if (wildcardType != null && wildcardType.containsTypeVariable()) {
			ArgType newWildcardType = replaceTypeVariablesUsingMap(wildcardType, replaceMap);
			if (newWildcardType == null) {
				return null;
			}
			return ArgType.wildcard(newWildcardType, replaceType.getWildcardBound());
		}

		List<ArgType> genericTypes = replaceType.getGenericTypes();
		if (replaceType.isGeneric() && notEmpty(genericTypes)) {
			List<ArgType> newTypes = Utils.collectionMap(genericTypes, t -> {
				ArgType type = replaceTypeVariablesUsingMap(t, replaceMap);
				return type == null ? t : type;
			});
			return ArgType.generic(replaceType, newTypes);
		}
		return null;
	}
}
