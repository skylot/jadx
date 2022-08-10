package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
			ArgType superClass = validateClsType(cls, sp.consumeType(), cls.getSuperClass());
			List<ArgType> interfaces = cls.getInterfaces();
			for (int i = 0; i < interfaces.size(); i++) {
				ArgType type = sp.consumeType();
				if (type != null) {
					interfaces.set(i, validateClsType(cls, type, interfaces.get(i)));
				} else {
					break;
				}
			}
			cls.updateGenericClsData(superClass, interfaces, generics);
		} catch (Exception e) {
			cls.addWarnComment("Failed to parse class signature: " + sp.getSignature(), e);
		}
	}

	private ArgType validateClsType(ClassNode cls, ArgType candidateType, ArgType currentType) {
		if (!candidateType.isObject()) {
			cls.addWarnComment("Incorrect class signature, class is not object: " + SignatureParser.getSignature(cls));
			return currentType;
		}
		if (Objects.equals(candidateType.getObject(), cls.getClassInfo().getType().getObject())) {
			cls.addWarnComment("Incorrect class signature, class is equals to this class: " + SignatureParser.getSignature(cls));
			return currentType;
		}
		return candidateType;
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
				ArrayList<ArgType> newArgTypes = new ArrayList<>(parsedArgTypes);
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
	public String toString() {
		return "SignatureProcessor";
	}
}
