package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.nodes.parser.SignatureParser;
import jadx.core.dex.nodes.utils.TypeUtils;
import jadx.core.dex.visitors.typeinference.TypeCompareEnum;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxException;

public class SignatureProcessor extends AbstractVisitor {
	private RootNode root;

	@Override
	public void init(RootNode root) {
		this.root = root;
	}

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		parseClassSignature(cls);
		for (FieldNode field : cls.getFields()) {
			parseFieldSignature(field);
		}
		for (MethodNode mth : cls.getMethods()) {
			parseMethodSignature(mth);
		}
		return true;
	}

	private void parseClassSignature(ClassNode cls) {
		SignatureParser sp = SignatureParser.fromNode(cls);
		if (sp == null) {
			return;
		}
		try {
			List<ArgType> generics = sp.consumeGenericTypeParameters();
			ArgType superClass = processSuperType(cls, sp.consumeType());
			List<ArgType> interfaces = processInterfaces(cls, sp.consumeTypeList());
			List<ArgType> resultGenerics = fixTypeParamDeclarations(cls, generics, superClass, interfaces);
			cls.updateGenericClsData(resultGenerics, superClass, interfaces);
		} catch (Exception e) {
			cls.addWarnComment("Failed to parse class signature: " + sp.getSignature(), e);
		}
	}

	private ArgType processSuperType(ClassNode cls, ArgType parsedType) {
		ArgType superType = cls.getSuperClass();
		if (Objects.equals(parsedType.getObject(), cls.getClassInfo().getType().getObject())) {
			cls.addWarnComment("Incorrect class signature: super class is equals to this class");
			return superType;
		}
		return bestClsType(cls, parsedType, superType);
	}

	/**
	 * Parse, validate and update class interfaces types.
	 */
	private List<ArgType> processInterfaces(ClassNode cls, List<ArgType> parsedTypes) {
		List<ArgType> interfaces = cls.getInterfaces();
		if (parsedTypes.isEmpty()) {
			return interfaces;
		}
		int parsedCount = parsedTypes.size();
		int interfacesCount = interfaces.size();
		List<ArgType> result = new ArrayList<>(interfacesCount);
		int count = Math.min(interfacesCount, parsedCount);
		for (int i = 0; i < interfacesCount; i++) {
			if (i < count) {
				result.add(bestClsType(cls, parsedTypes.get(i), interfaces.get(i)));
			} else {
				result.add(interfaces.get(i));
			}
		}
		if (interfacesCount < parsedCount) {
			cls.addWarnComment("Unexpected interfaces in signature: " + parsedTypes.subList(interfacesCount, parsedCount));
		}
		return result;
	}

	/**
	 * Add missing type parameters from super type and interfaces to make code compilable
	 */
	private static List<ArgType> fixTypeParamDeclarations(ClassNode cls,
			List<ArgType> generics, ArgType superClass, List<ArgType> interfaces) {
		if (interfaces.isEmpty() && superClass.equals(ArgType.OBJECT)) {
			return generics;
		}
		Set<String> typeParams = new HashSet<>();
		superClass.visitTypes(t -> addGenericType(typeParams, t));
		interfaces.forEach(i -> i.visitTypes(t -> addGenericType(typeParams, t)));
		if (typeParams.isEmpty()) {
			return generics;
		}
		List<ArgType> knownTypeParams;
		if (cls.isInner()) {
			knownTypeParams = new ArrayList<>(generics);
			cls.visitParentClasses(p -> knownTypeParams.addAll(p.getGenericTypeParameters()));
		} else {
			knownTypeParams = generics;
		}
		for (ArgType declTypeParam : knownTypeParams) {
			typeParams.remove(declTypeParam.getObject());
		}
		if (typeParams.isEmpty()) {
			return generics;
		}
		cls.addInfoComment("Add missing generic type declarations: " + typeParams);
		List<ArgType> fixedGenerics = new ArrayList<>(generics.size() + typeParams.size());
		fixedGenerics.addAll(generics);
		typeParams.stream().sorted().map(ArgType::genericType).forEach(fixedGenerics::add);
		return fixedGenerics;
	}

	private static @Nullable Object addGenericType(Set<String> usedTypeParameters, ArgType t) {
		if (t.isGenericType()) {
			usedTypeParameters.add(t.getObject());
		}
		return null;
	}

	private ArgType bestClsType(ClassNode cls, ArgType candidateType, ArgType currentType) {
		if (validateClsType(cls, candidateType)) {
			return candidateType;
		}
		return currentType;
	}

	private boolean validateClsType(ClassNode cls, ArgType candidateType) {
		if (candidateType == null) {
			return false;
		}
		if (!candidateType.isObject()) {
			cls.addWarnComment("Incorrect class signature, class is not an object: " + candidateType);
			return false;
		}
		return true;
	}

	private void parseFieldSignature(FieldNode field) {
		SignatureParser sp = SignatureParser.fromNode(field);
		if (sp == null) {
			return;
		}
		ClassNode cls = field.getParentClass();
		try {
			ArgType signatureType = sp.consumeType();
			if (signatureType == null) {
				return;
			}
			if (isNotGenericType(field.getFieldInfo().getType())) {
				field.addWarnComment("Field that had annotation with different signature: " + signatureType);
				return;
			}
			if (!validateInnerType(signatureType)) {
				field.addWarnComment("Incorrect inner types in field signature: " + sp.getSignature());
				return;
			}
			ArgType type = root.getTypeUtils().expandTypeVariables(cls, signatureType);
			if (!validateParsedType(type, field.getType())) {
				cls.addWarnComment("Incorrect field signature: " + sp.getSignature());
				return;
			}
			field.updateType(type);
		} catch (Exception e) {
			cls.addWarnComment("Field signature parse error: " + field.getName(), e);
		}
	}

	private boolean isNotGenericType(ArgType ty) {
		if (ty.isPrimitive()) {
			// Primitives do not get resolved to the annotation type, so we can just skip them altogether
			return true;
		}
		String tyToString = ty.toString();
		final String[] stdNonGenericTypes = {
				"bool",
				"byte",
				"short",
				"char",
				"float",
				"int",
				"long",
				"double",

				"java.lang.String",
				"java.lang.Boolean",
				"java.lang.Byte",
				"java.lang.Long",
				"java.lang.Short",
				"java.lang.Number",
				"java.lang.CharSequence",
				"java.lang.Double",
				"java.lang.Float",
		};
		for (String genericType : stdNonGenericTypes) {
			if (genericType.equals(tyToString)) {
				return true;
			}
		}
		return false;
	}

	private void parseMethodSignature(MethodNode mth) {
		SignatureParser sp = SignatureParser.fromNode(mth);
		if (sp == null) {
			return;
		}
		try {
			List<ArgType> typeParameters = sp.consumeGenericTypeParameters();
			List<ArgType> parsedArgTypes = sp.consumeMethodArgs(mth.getMethodInfo().getArgsCount());
			ArgType parsedRetType = sp.consumeType();

			if (!validateInnerType(parsedRetType) || !validateInnerType(parsedArgTypes)) {
				mth.addWarnComment("Incorrect inner types in method signature: " + sp.getSignature());
				return;
			}

			mth.updateTypeParameters(typeParameters); // apply before expand args
			TypeUtils typeUtils = root.getTypeUtils();
			ArgType retType = typeUtils.expandTypeVariables(mth, parsedRetType);
			List<ArgType> argTypes = Utils.collectionMap(parsedArgTypes, t -> typeUtils.expandTypeVariables(mth, t));

			if (!validateAndApplyTypes(mth, sp, retType, argTypes)) {
				// bad types -> reset typed parameters
				mth.updateTypeParameters(Collections.emptyList());
			}
		} catch (Exception e) {
			mth.addWarnComment("Failed to parse method signature: " + sp.getSignature(), e);
		}
	}

	private boolean validateAndApplyTypes(MethodNode mth, SignatureParser sp, ArgType retType, List<ArgType> argTypes) {
		try {
			if (!validateParsedType(retType, mth.getMethodInfo().getReturnType())) {
				mth.addWarnComment("Incorrect return type in method signature: " + sp.getSignature());
				return false;
			}
			List<ArgType> checkedArgTypes = checkArgTypes(mth, sp, argTypes);
			if (checkedArgTypes == null) {
				return false;
			}
			mth.updateTypes(Collections.unmodifiableList(checkedArgTypes), retType);
			return true;
		} catch (Exception e) {
			mth.addWarnComment("Type validation failed for signature: " + sp.getSignature(), e);
			return false;
		}
	}

	private List<ArgType> checkArgTypes(MethodNode mth, SignatureParser sp, List<ArgType> parsedArgTypes) {
		MethodInfo mthInfo = mth.getMethodInfo();
		List<ArgType> mthArgTypes = mthInfo.getArgumentsTypes();
		int len = parsedArgTypes.size();
		if (len != mthArgTypes.size()) {
			if (mth.getParentClass().getAccessFlags().isEnum()) {
				// ignore for enums
				return null;
			}
			if (mthInfo.isConstructor() && !mthArgTypes.isEmpty() && !parsedArgTypes.isEmpty()) {
				// add synthetic arg for outer class (see test TestGeneric8)
				List<ArgType> newArgTypes = new ArrayList<>(parsedArgTypes);
				newArgTypes.add(0, mthArgTypes.get(0));
				if (newArgTypes.size() == mthArgTypes.size()) {
					return newArgTypes;
				}
			}
			mth.addDebugComment("Incorrect args count in method signature: " + sp.getSignature());
			return null;
		}
		for (int i = 0; i < len; i++) {
			ArgType parsedType = parsedArgTypes.get(i);
			ArgType mthArgType = mthArgTypes.get(i);
			if (!validateParsedType(parsedType, mthArgType)) {
				mth.addWarnComment("Incorrect types in method signature: " + sp.getSignature());
				return null;
			}
		}
		return parsedArgTypes;
	}

	private boolean validateParsedType(ArgType parsedType, ArgType currentType) {
		TypeCompareEnum result = root.getTypeCompare().compareTypes(parsedType, currentType);
		return result != TypeCompareEnum.CONFLICT;
	}

	private boolean validateInnerType(List<ArgType> types) {
		for (ArgType type : types) {
			if (!validateInnerType(type)) {
				return false;
			}
		}
		return true;
	}

	private boolean validateInnerType(ArgType type) {
		ArgType innerType = type.getInnerType();
		if (innerType == null) {
			return true;
		}
		// check in outer type has inner type as inner class
		ArgType outerType = type.getOuterType();
		ClassNode outerCls = root.resolveClass(outerType);
		if (outerCls == null) {
			// can't check class not found
			return true;
		}
		String innerObj;
		if (innerType.getOuterType() != null) {
			innerObj = innerType.getOuterType().getObject();
			// "next" inner type will be processed at end of method
		} else {
			innerObj = innerType.getObject();
		}
		if (!innerObj.contains(".")) {
			// short reference
			for (ClassNode innerClass : outerCls.getInnerClasses()) {
				if (innerClass.getShortName().equals(innerObj)) {
					return true;
				}
			}
			return false;
		}
		// full name
		ClassNode innerCls = root.resolveClass(innerObj);
		if (innerCls == null) {
			return false;
		}
		if (!innerCls.getParentClass().equals(outerCls)) {
			// not inner => fixing
			outerCls.addInnerClass(innerCls);
			innerCls.getClassInfo().convertToInner(outerCls);
		}
		return validateInnerType(innerType);
	}

	@Override
	public String getName() {
		return "SignatureProcessor";
	}
}
