package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.Consts;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.BaseInvokeNode;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IMethodDetails;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.nodes.utils.TypeUtils;
import jadx.core.dex.visitors.methods.MutableMethodDetails;
import jadx.core.dex.visitors.shrink.CodeShrinkVisitor;
import jadx.core.dex.visitors.typeinference.TypeCompare;
import jadx.core.dex.visitors.typeinference.TypeCompareEnum;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;

@JadxVisitor(
		name = "MethodInvokeVisitor",
		desc = "Process additional info for method invocation (overload, vararg)",
		runAfter = {
				CodeShrinkVisitor.class,
				ModVisitor.class
		},
		runBefore = {
				SimplifyVisitor.class // run before cast remove and StringBuilder replace
		}
)
public class MethodInvokeVisitor extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(MethodInvokeVisitor.class);

	private RootNode root;

	@Override
	public void init(RootNode root) {
		this.root = root;
	}

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}
		for (BlockNode block : mth.getBasicBlocks()) {
			if (block.contains(AFlag.DONT_GENERATE)) {
				continue;
			}
			for (InsnNode insn : block.getInstructions()) {
				if (insn.contains(AFlag.DONT_GENERATE)) {
					continue;
				}
				insn.visitInsns(in -> {
					if (in instanceof BaseInvokeNode) {
						processInvoke(mth, ((BaseInvokeNode) in));
					}
				});
			}
		}
	}

	private void processInvoke(MethodNode parentMth, BaseInvokeNode invokeInsn) {
		MethodInfo callMth = invokeInsn.getCallMth();
		if (callMth.getArgsCount() == 0) {
			return;
		}
		IMethodDetails mthDetails = root.getMethodUtils().getMethodDetails(invokeInsn);
		if (mthDetails == null) {
			if (Consts.DEBUG) {
				parentMth.addDebugComment("Method info not found: " + callMth);
			}
			processUnknown(invokeInsn);
		} else {
			if (mthDetails.isVarArg()) {
				ArgType last = Utils.last(mthDetails.getArgTypes());
				if (last != null && last.isArray()) {
					invokeInsn.add(AFlag.VARARG_CALL);
				}
			}
			processOverloaded(parentMth, invokeInsn, mthDetails);
		}
	}

	private void processOverloaded(MethodNode parentMth, BaseInvokeNode invokeInsn, IMethodDetails mthDetails) {
		MethodInfo callMth = invokeInsn.getCallMth();
		ArgType callCls = getCallClassFromInvoke(parentMth, invokeInsn, callMth);
		List<IMethodDetails> overloadMethods = root.getMethodUtils().collectOverloadedMethods(callCls, callMth);
		if (overloadMethods.isEmpty()) {
			// not overloaded
			return;
		}

		// resolve generic type variables
		Map<ArgType, ArgType> typeVarsMapping = getTypeVarsMapping(invokeInsn);
		IMethodDetails effectiveMthDetails = resolveTypeVars(mthDetails, typeVarsMapping);
		List<IMethodDetails> effectiveOverloadMethods = new ArrayList<>(overloadMethods.size() + 1);
		for (IMethodDetails overloadMethod : overloadMethods) {
			effectiveOverloadMethods.add(resolveTypeVars(overloadMethod, typeVarsMapping));
		}
		effectiveOverloadMethods.add(effectiveMthDetails);

		// search cast types to resolve overloading
		int argsOffset = invokeInsn.getFirstArgOffset();
		List<ArgType> compilerVarTypes = collectCompilerVarTypes(invokeInsn, argsOffset);
		List<ArgType> castTypes = searchCastTypes(parentMth, effectiveMthDetails, effectiveOverloadMethods, compilerVarTypes);
		List<ArgType> resultCastTypes = expandTypes(parentMth, effectiveMthDetails, castTypes);
		applyArgsCast(invokeInsn, argsOffset, compilerVarTypes, resultCastTypes);
	}

	/**
	 * Method details not found => add cast for 'null' args
	 */
	private void processUnknown(BaseInvokeNode invokeInsn) {
		int argsOffset = invokeInsn.getFirstArgOffset();
		List<ArgType> compilerVarTypes = collectCompilerVarTypes(invokeInsn, argsOffset);
		List<ArgType> castTypes = new ArrayList<>(compilerVarTypes);
		if (replaceUnknownTypes(castTypes, invokeInsn.getCallMth().getArgumentsTypes())) {
			applyArgsCast(invokeInsn, argsOffset, compilerVarTypes, castTypes);
		}
	}

	private ArgType getCallClassFromInvoke(MethodNode parentMth, BaseInvokeNode invokeInsn, MethodInfo callMth) {
		if (invokeInsn instanceof ConstructorInsn) {
			ConstructorInsn constrInsn = (ConstructorInsn) invokeInsn;
			if (constrInsn.isSuper()) {
				return parentMth.getParentClass().getSuperClass();
			}
		}
		InsnArg instanceArg = invokeInsn.getInstanceArg();
		if (instanceArg != null) {
			return instanceArg.getType();
		}
		// static call
		return callMth.getDeclClass().getType();
	}

	private Map<ArgType, ArgType> getTypeVarsMapping(BaseInvokeNode invokeInsn) {
		MethodInfo callMthInfo = invokeInsn.getCallMth();
		ArgType declClsType = callMthInfo.getDeclClass().getType();
		ArgType callClsType = getClsCallType(invokeInsn, declClsType);

		TypeUtils typeUtils = root.getTypeUtils();
		Map<ArgType, ArgType> clsTypeVars = typeUtils.getTypeVariablesMapping(callClsType);
		Map<ArgType, ArgType> mthTypeVars = typeUtils.getTypeVarMappingForInvoke(invokeInsn);
		return Utils.mergeMaps(clsTypeVars, mthTypeVars);
	}

	private ArgType getClsCallType(BaseInvokeNode invokeInsn, ArgType declClsType) {
		InsnArg instanceArg = invokeInsn.getInstanceArg();
		if (instanceArg != null) {
			return instanceArg.getType();
		}
		if (invokeInsn.getType() == InsnType.CONSTRUCTOR && invokeInsn.getResult() != null) {
			return invokeInsn.getResult().getType();
		}
		return declClsType;
	}

	private void applyArgsCast(BaseInvokeNode invokeInsn, int argsOffset, List<ArgType> compilerVarTypes, List<ArgType> castTypes) {
		int argsCount = invokeInsn.getArgsCount();
		for (int i = argsOffset; i < argsCount; i++) {
			InsnArg arg = invokeInsn.getArg(i);
			int origPos = i - argsOffset;
			ArgType compilerType = compilerVarTypes.get(origPos);
			ArgType castType = castTypes.get(origPos);
			if (castType != null) {
				if (!castType.equals(compilerType)) {
					if (arg.isLiteral() && compilerType.isPrimitive() && castType.isPrimitive()) {
						arg.setType(castType);
						arg.add(AFlag.EXPLICIT_PRIMITIVE_TYPE);
					} else if (InsnUtils.isWrapped(arg, InsnType.CHECK_CAST)) {
						IndexInsnNode wrapInsn = ((IndexInsnNode) ((InsnWrapArg) arg).getWrapInsn());
						wrapInsn.updateIndex(castType);
					} else {
						if (Consts.DEBUG_TYPE_INFERENCE) {
							LOG.info("Insert cast for invoke insn arg: {}, insn: {}", arg, invokeInsn);
						}
						InsnNode castInsn = new IndexInsnNode(InsnType.CAST, castType, 1);
						castInsn.addArg(arg);
						castInsn.add(AFlag.EXPLICIT_CAST);
						InsnArg wrapCast = InsnArg.wrapArg(castInsn);
						wrapCast.setType(castType);
						invokeInsn.setArg(i, wrapCast);
					}
				} else {
					// protect already existed cast
					if (arg.isInsnWrap()) {
						InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
						if (wrapInsn.getType() == InsnType.CHECK_CAST) {
							wrapInsn.add(AFlag.EXPLICIT_CAST);
						}
					}
				}
			}
		}
	}

	private IMethodDetails resolveTypeVars(IMethodDetails mthDetails, Map<ArgType, ArgType> typeVarsMapping) {
		List<ArgType> argTypes = mthDetails.getArgTypes();
		int argsCount = argTypes.size();
		boolean fixed = false;
		List<ArgType> fixedArgTypes = new ArrayList<>(argsCount);
		for (int argNum = 0; argNum < argsCount; argNum++) {
			ArgType argType = argTypes.get(argNum);
			if (argType == null) {
				throw new JadxRuntimeException("Null arg type in " + mthDetails + " at: " + argNum + " in: " + argTypes);
			}
			if (argType.containsTypeVariable()) {
				ArgType resolvedType = root.getTypeUtils().replaceTypeVariablesUsingMap(argType, typeVarsMapping);
				if (resolvedType == null || resolvedType.equals(argType)) {
					// type variables erased from method info by compiler
					resolvedType = mthDetails.getMethodInfo().getArgumentsTypes().get(argNum);
				}
				fixedArgTypes.add(resolvedType);
				fixed = true;
			} else {
				fixedArgTypes.add(argType);
			}
		}
		ArgType returnType = mthDetails.getReturnType();
		if (returnType.containsTypeVariable()) {
			ArgType resolvedType = root.getTypeUtils().replaceTypeVariablesUsingMap(returnType, typeVarsMapping);
			if (resolvedType == null || resolvedType.containsTypeVariable()) {
				returnType = mthDetails.getMethodInfo().getReturnType();
				fixed = true;
			}
		}

		if (!fixed) {
			return mthDetails;
		}
		MutableMethodDetails mutableMethodDetails = new MutableMethodDetails(mthDetails);
		mutableMethodDetails.setArgTypes(fixedArgTypes);
		mutableMethodDetails.setRetType(returnType);
		return mutableMethodDetails;
	}

	private List<ArgType> searchCastTypes(MethodNode parentMth, IMethodDetails mthDetails, List<IMethodDetails> overloadedMethods,
			List<ArgType> compilerVarTypes) {
		// try compiler types
		if (isOverloadResolved(mthDetails, overloadedMethods, compilerVarTypes)) {
			return compilerVarTypes;
		}
		int argsCount = compilerVarTypes.size();
		List<ArgType> castTypes = new ArrayList<>(compilerVarTypes);

		// replace unknown types
		boolean changed = replaceUnknownTypes(castTypes, mthDetails.getArgTypes());
		if (changed && isOverloadResolved(mthDetails, overloadedMethods, castTypes)) {
			return castTypes;
		}

		// replace generic types
		changed = false;
		for (int i = 0; i < argsCount; i++) {
			ArgType castType = castTypes.get(i);
			ArgType mthType = mthDetails.getArgTypes().get(i);
			if (!castType.isGeneric() && mthType.isGeneric()) {
				castTypes.set(i, mthType);
				changed = true;
			}
		}
		if (changed && isOverloadResolved(mthDetails, overloadedMethods, castTypes)) {
			return castTypes;
		}

		// if just one arg => cast will resolve
		if (argsCount == 1) {
			return mthDetails.getArgTypes();
		}
		if (Consts.DEBUG_OVERLOADED_CASTS) {
			// TODO: try to minimize casts count
			parentMth.addDebugComment("Failed to find minimal casts for resolve overloaded methods, cast all args instead"
					+ "\n method: " + mthDetails
					+ "\n arg types: " + compilerVarTypes
					+ "\n candidates:"
					+ "\n  " + Utils.listToString(overloadedMethods, "\n  "));
		}
		// not resolved -> cast all args
		return mthDetails.getArgTypes();
	}

	private boolean replaceUnknownTypes(List<ArgType> castTypes, List<ArgType> mthArgTypes) {
		int argsCount = castTypes.size();
		boolean changed = false;
		for (int i = 0; i < argsCount; i++) {
			ArgType castType = castTypes.get(i);
			if (!castType.isTypeKnown()) {
				ArgType mthType = mthArgTypes.get(i);
				castTypes.set(i, mthType);
				changed = true;
			}
		}
		return changed;
	}

	/**
	 * Use generified types if available
	 */
	private List<ArgType> expandTypes(MethodNode parentMth, IMethodDetails methodDetails, List<ArgType> castTypes) {
		TypeCompare typeCompare = parentMth.root().getTypeCompare();
		List<ArgType> mthArgTypes = methodDetails.getArgTypes();
		int argsCount = castTypes.size();
		List<ArgType> list = new ArrayList<>(argsCount);
		for (int i = 0; i < argsCount; i++) {
			ArgType mthType = mthArgTypes.get(i);
			ArgType castType = castTypes.get(i);
			TypeCompareEnum result = typeCompare.compareTypes(mthType, castType);
			if (result == TypeCompareEnum.NARROW_BY_GENERIC) {
				list.add(mthType);
			} else {
				list.add(castType);
			}
		}
		return list;
	}

	private boolean isOverloadResolved(IMethodDetails expectedMthDetails, List<IMethodDetails> overloadedMethods, List<ArgType> castTypes) {
		if (overloadedMethods.isEmpty()) {
			return false;
		}
		// TODO: search closest method, instead filtering
		List<IMethodDetails> strictMethods = filterApplicableMethods(overloadedMethods, castTypes, MethodInvokeVisitor::isStrictTypes);
		if (strictMethods.size() == 1) {
			return strictMethods.get(0).equals(expectedMthDetails);
		}
		List<IMethodDetails> resolvedMethods = filterApplicableMethods(overloadedMethods, castTypes, MethodInvokeVisitor::isTypeApplicable);
		if (resolvedMethods.size() == 1) {
			return resolvedMethods.get(0).equals(expectedMthDetails);
		}
		return false;
	}

	private static boolean isStrictTypes(TypeCompareEnum result) {
		return result.isEqual();
	}

	private static boolean isTypeApplicable(TypeCompareEnum result) {
		return result.isNarrowOrEqual() || result == TypeCompareEnum.WIDER_BY_GENERIC;
	}

	private List<IMethodDetails> filterApplicableMethods(List<IMethodDetails> methods, List<ArgType> types,
			Function<TypeCompareEnum, Boolean> acceptFunction) {
		List<IMethodDetails> list = new ArrayList<>(methods.size());
		for (IMethodDetails m : methods) {
			if (isMethodAcceptable(m, types, acceptFunction)) {
				list.add(m);
			}
		}
		return list;
	}

	private boolean isMethodAcceptable(IMethodDetails methodDetails, List<ArgType> types,
			Function<TypeCompareEnum, Boolean> acceptFunction) {
		List<ArgType> mthTypes = methodDetails.getArgTypes();
		int argCount = mthTypes.size();
		if (argCount != types.size()) {
			return false;
		}
		TypeCompare typeCompare = root.getTypeUpdate().getTypeCompare();
		for (int i = 0; i < argCount; i++) {
			ArgType mthType = mthTypes.get(i);
			ArgType argType = types.get(i);
			TypeCompareEnum result = typeCompare.compareTypes(argType, mthType);
			if (!acceptFunction.apply(result)) {
				return false;
			}
		}
		return true;
	}

	private List<ArgType> collectCompilerVarTypes(BaseInvokeNode insn, int argOffset) {
		int argsCount = insn.getArgsCount();
		List<ArgType> result = new ArrayList<>(argsCount);
		for (int i = argOffset; i < argsCount; i++) {
			InsnArg arg = insn.getArg(i);
			result.add(getCompilerVarType(arg));
		}
		return result;
	}

	/**
	 * Return type as seen by compiler
	 */
	private ArgType getCompilerVarType(InsnArg arg) {
		if (arg instanceof LiteralArg) {
			LiteralArg literalArg = (LiteralArg) arg;
			ArgType type = literalArg.getType();
			if (literalArg.getLiteral() == 0) {
				if (type.isObject() || type.isArray()) {
					// null
					return ArgType.UNKNOWN_OBJECT;
				}
			}
			if (type.isPrimitive() && !arg.contains(AFlag.EXPLICIT_PRIMITIVE_TYPE)) {
				return ArgType.INT;
			}
			return arg.getType();
		}
		if (arg instanceof RegisterArg) {
			return arg.getType();
		}
		if (arg instanceof InsnWrapArg) {
			InsnWrapArg wrapArg = (InsnWrapArg) arg;
			return getInsnCompilerType(arg, wrapArg.getWrapInsn());
		}
		throw new JadxRuntimeException("Unknown var type for: " + arg);
	}

	private static ArgType getInsnCompilerType(InsnArg arg, InsnNode insn) {
		switch (insn.getType()) {
			case CAST:
			case CHECK_CAST:
				return ((IndexInsnNode) insn).getIndexAsType();

			default:
				if (insn.getResult() != null) {
					return insn.getResult().getType();
				}
				return arg.getType();
		}
	}
}
