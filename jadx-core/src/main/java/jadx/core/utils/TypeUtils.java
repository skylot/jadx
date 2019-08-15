package jadx.core.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.CallMthInterface;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.GenericInfo;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class TypeUtils {
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
	public static ArgType replaceClassGenerics(RootNode root, ArgType instanceType, ArgType typeWithGeneric) {
		if (typeWithGeneric == null) {
			return null;
		}
		if (instanceType.isGeneric()) {
			List<GenericInfo> generics = root.getClassGenerics(instanceType);
			if (generics.isEmpty()) {
				return null;
			}
			ArgType[] actualTypes = instanceType.getGenericTypes();
			if (actualTypes == null) {
				return null;
			}
			int genericParamsCount = actualTypes.length;
			if (genericParamsCount != generics.size()) {
				return null;
			}
			Map<ArgType, ArgType> replaceMap = new HashMap<>(genericParamsCount);
			for (int i = 0; i < genericParamsCount; i++) {
				ArgType actualType = actualTypes[i];
				ArgType genericType = generics.get(i).getGenericType();
				replaceMap.put(genericType, actualType);
			}
			return replaceGenericUsingTypeMap(typeWithGeneric, replaceMap);
		}
		return null;
	}

	@Nullable
	public static ArgType replaceMethodGenerics(RootNode root, InsnNode invokeInsn, ArgType typeWithGeneric) {
		if (typeWithGeneric == null) {
			return null;
		}
		if (!(invokeInsn instanceof CallMthInterface)) {
			throw new JadxRuntimeException("Expected CallMthInterface, got: " + invokeInsn.getClass());
		}
		CallMthInterface callInsn = (CallMthInterface) invokeInsn;
		MethodInfo mthInfo = callInsn.getCallMth();
		List<ArgType> methodArgTypes = root.getMethodArgTypes(mthInfo);
		if (methodArgTypes.isEmpty()) {
			return null;
		}
		int firstArgOffset = callInsn.getFirstArgOffset();
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

	private static ArgType replaceGenericUsingTypeMap(ArgType replaceType, Map<ArgType, ArgType> replaceMap) {
		if (replaceType.isGenericType()) {
			return replaceMap.get(replaceType);
		}

		ArgType wildcardType = replaceType.getWildcardType();
		if (wildcardType != null && wildcardType.containsGenericType()) {
			ArgType newWildcardType = replaceGenericUsingTypeMap(wildcardType, replaceMap);
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
				ArgType type = replaceGenericUsingTypeMap(genericType, replaceMap);
				if (type == null) {
					type = genericType;
				}
				newTypes[i] = type;
			}
			return ArgType.generic(replaceType.getObject(), newTypes);
		}
		return null;
	}

	private TypeUtils() {
	}
}
