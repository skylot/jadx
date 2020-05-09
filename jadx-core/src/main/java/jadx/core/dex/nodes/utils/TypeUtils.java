package jadx.core.dex.nodes.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.core.clsp.ClspClass;
import jadx.core.dex.instructions.BaseInvokeNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.GenericTypeParameter;
import jadx.core.dex.nodes.IMethodDetails;
import jadx.core.dex.nodes.RootNode;

public class TypeUtils {
	private final RootNode root;

	public TypeUtils(RootNode rootNode) {
		this.root = rootNode;
	}

	@NotNull
	public List<GenericTypeParameter> getClassGenerics(ArgType type) {
		ClassNode classNode = root.resolveClass(type);
		if (classNode != null) {
			return classNode.getGenericTypeParameters();
		}
		ClspClass clsDetails = root.getClsp().getClsDetails(type);
		if (clsDetails == null || clsDetails.getTypeParameters().isEmpty()) {
			return Collections.emptyList();
		}
		List<GenericTypeParameter> generics = clsDetails.getTypeParameters();
		return generics == null ? Collections.emptyList() : generics;
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
		if (typeWithGeneric != null) {
			Map<ArgType, ArgType> replaceMap = getTypeVariablesMapping(instanceType);
			if (!replaceMap.isEmpty()) {
				return replaceTypeVariablesUsingMap(typeWithGeneric, replaceMap);
			}
		}
		return null;
	}

	public Map<ArgType, ArgType> getTypeVariablesMapping(ArgType clsType) {
		if (!clsType.isGeneric()) {
			return Collections.emptyMap();
		}

		List<GenericTypeParameter> typeParameters = root.getTypeUtils().getClassGenerics(clsType);
		if (typeParameters.isEmpty()) {
			return Collections.emptyMap();
		}
		ArgType[] actualTypes = clsType.getGenericTypes();
		if (actualTypes == null) {
			return Collections.emptyMap();
		}
		int genericParamsCount = actualTypes.length;
		if (genericParamsCount != typeParameters.size()) {
			return Collections.emptyMap();
		}
		Map<ArgType, ArgType> replaceMap = new HashMap<>(genericParamsCount);
		for (int i = 0; i < genericParamsCount; i++) {
			ArgType actualType = actualTypes[i];
			ArgType genericType = typeParameters.get(i).getTypeVariable();
			replaceMap.put(genericType, actualType);
		}
		return replaceMap;
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

		ArgType wildcardType = replaceType.getWildcardType();
		if (wildcardType != null && wildcardType.containsTypeVariable()) {
			ArgType newWildcardType = replaceTypeVariablesUsingMap(wildcardType, replaceMap);
			if (newWildcardType == null) {
				return null;
			}
			return ArgType.wildcard(newWildcardType, replaceType.getWildcardBound());
		}

		ArgType[] genericTypes = replaceType.getGenericTypes();
		if (replaceType.isGeneric() && genericTypes != null && genericTypes.length != 0) {
			int size = genericTypes.length;
			ArgType[] newTypes = new ArgType[size];
			for (int i = 0; i < size; i++) {
				ArgType genericType = genericTypes[i];
				ArgType type = replaceTypeVariablesUsingMap(genericType, replaceMap);
				if (type == null) {
					type = genericType;
				}
				newTypes[i] = type;
			}
			return ArgType.generic(replaceType.getObject(), newTypes);
		}
		return null;
	}
}
