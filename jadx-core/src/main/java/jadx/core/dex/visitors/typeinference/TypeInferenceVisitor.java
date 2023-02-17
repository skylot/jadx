package jadx.core.dex.visitors.typeinference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.Consts;
import jadx.core.clsp.ClspGraph;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.AnonymousClassAttr;
import jadx.core.dex.attributes.nodes.PhiListAttr;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.instructions.ArithNode;
import jadx.core.dex.instructions.ArithOp;
import jadx.core.dex.instructions.BaseInvokeNode;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.InvokeType;
import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.PrimitiveType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.instructions.mods.TernaryInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.IMethodDetails;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.nodes.utils.MethodUtils;
import jadx.core.dex.trycatch.ExcHandlerAttr;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.AttachMethodDetails;
import jadx.core.dex.visitors.ConstInlineVisitor;
import jadx.core.dex.visitors.InitCodeVariables;
import jadx.core.dex.visitors.JadxVisitor;
import jadx.core.dex.visitors.ModVisitor;
import jadx.core.dex.visitors.blocks.BlockSplitter;
import jadx.core.dex.visitors.ssa.SSATransform;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InsnList;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.ListUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxOverflowException;

@JadxVisitor(
		name = "Type Inference",
		desc = "Calculate best types for SSA variables",
		runAfter = {
				SSATransform.class,
				ConstInlineVisitor.class,
				AttachMethodDetails.class
		}
)
public final class TypeInferenceVisitor extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(TypeInferenceVisitor.class);

	private RootNode root;
	private TypeUpdate typeUpdate;
	private List<Function<MethodNode, Boolean>> resolvers;

	@Override
	public void init(RootNode root) {
		this.root = root;
		this.typeUpdate = root.getTypeUpdate();
		this.resolvers = Arrays.asList(
				this::initTypeBounds,
				this::runTypePropagation,
				this::tryRestoreTypeVarCasts,
				this::tryInsertCasts,
				this::tryDeduceTypes,
				this::trySplitConstInsns,
				this::tryToFixIncompatiblePrimitives,
				this::tryToForceImmutableTypes,
				this::tryInsertAdditionalMove,
				this::runMultiVariableSearch,
				this::tryRemoveGenerics);
	}

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}
		if (Consts.DEBUG_TYPE_INFERENCE) {
			LOG.info("Start type inference in method: {}", mth);
		}
		assignImmutableTypes(mth);
		try {
			for (Function<MethodNode, Boolean> resolver : resolvers) {
				if (resolver.apply(mth) && checkTypes(mth)) {
					return;
				}
			}
		} catch (Exception e) {
			mth.addError("Type inference failed with exception", e);
		}
	}

	/**
	 * Check if all types resolved
	 */
	private static boolean checkTypes(MethodNode mth) {
		for (SSAVar var : mth.getSVars()) {
			ArgType type = var.getTypeInfo().getType();
			if (!type.isTypeKnown()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Collect initial type bounds from assign and usages
	 */
	private boolean initTypeBounds(MethodNode mth) {
		List<SSAVar> ssaVars = mth.getSVars();
		ssaVars.forEach(this::attachBounds);
		ssaVars.forEach(this::mergePhiBounds);
		if (Consts.DEBUG_TYPE_INFERENCE) {
			ssaVars.forEach(ssaVar -> LOG.debug("Type bounds for {}: {}", ssaVar.toShortString(), ssaVar.getTypeInfo().getBounds()));
		}
		return false;
	}

	/**
	 * Guess type from usage and try to set it to current variable
	 * and all connected instructions with {@link TypeUpdate#apply(MethodNode, SSAVar, ArgType)}
	 */
	private boolean runTypePropagation(MethodNode mth) {
		List<SSAVar> ssaVars = mth.getSVars();
		ssaVars.forEach(var -> setImmutableType(mth, var));
		ssaVars.forEach(var -> setBestType(mth, var));
		return true;
	}

	private boolean runMultiVariableSearch(MethodNode mth) {
		try {
			TypeSearch typeSearch = new TypeSearch(mth);
			if (!typeSearch.run()) {
				mth.addWarnComment("Multi-variable type inference failed");
			}
			for (SSAVar var : mth.getSVars()) {
				if (!var.getTypeInfo().getType().isTypeKnown()) {
					return false;
				}
			}
			return true;
		} catch (Exception e) {
			mth.addWarnComment("Multi-variable type inference failed. Error: " + Utils.getStackTrace(e));
			return false;
		}
	}

	private void setImmutableType(MethodNode mth, SSAVar ssaVar) {
		try {
			ArgType immutableType = ssaVar.getImmutableType();
			if (immutableType != null) {
				TypeUpdateResult result = typeUpdate.applyWithWiderIgnSame(mth, ssaVar, immutableType);
				if (Consts.DEBUG_TYPE_INFERENCE && result == TypeUpdateResult.REJECT) {
					LOG.info("Reject initial immutable type {} for {}", immutableType, ssaVar);
				}
			}
		} catch (JadxOverflowException e) {
			throw e;
		} catch (Exception e) {
			LOG.error("Failed to set immutable type for var: {}", ssaVar, e);
		}
	}

	private boolean setBestType(MethodNode mth, SSAVar ssaVar) {
		try {
			return calculateFromBounds(mth, ssaVar);
		} catch (JadxOverflowException e) {
			throw e;
		} catch (Exception e) {
			LOG.error("Failed to calculate best type for var: {}", ssaVar, e);
			return false;
		}
	}

	private boolean calculateFromBounds(MethodNode mth, SSAVar ssaVar) {
		TypeInfo typeInfo = ssaVar.getTypeInfo();
		Set<ITypeBound> bounds = typeInfo.getBounds();
		Optional<ArgType> bestTypeOpt = selectBestTypeFromBounds(bounds);
		if (!bestTypeOpt.isPresent()) {
			if (Consts.DEBUG_TYPE_INFERENCE) {
				LOG.warn("Failed to select best type from bounds, count={} : ", bounds.size());
				for (ITypeBound bound : bounds) {
					LOG.warn("  {}", bound);
				}
			}
			return false;
		}
		ArgType candidateType = bestTypeOpt.get();
		TypeUpdateResult result = typeUpdate.apply(mth, ssaVar, candidateType);
		if (result == TypeUpdateResult.REJECT) {
			if (Consts.DEBUG_TYPE_INFERENCE) {
				if (ssaVar.getTypeInfo().getType().equals(candidateType)) {
					LOG.info("Same type rejected: {} -> {}, bounds: {}", ssaVar, candidateType, bounds);
				} else if (candidateType.isTypeKnown()) {
					LOG.debug("Type rejected: {} -> {}, bounds: {}", ssaVar, candidateType, bounds);
				}
			}
			return false;
		}
		return result == TypeUpdateResult.CHANGED;
	}

	private Optional<ArgType> selectBestTypeFromBounds(Set<ITypeBound> bounds) {
		return bounds.stream()
				.map(ITypeBound::getType)
				.filter(Objects::nonNull)
				.max(typeUpdate.getTypeCompare().getComparator());
	}

	private void attachBounds(SSAVar var) {
		TypeInfo typeInfo = var.getTypeInfo();
		typeInfo.getBounds().clear();
		RegisterArg assign = var.getAssign();
		addAssignBound(typeInfo, assign);

		for (RegisterArg regArg : var.getUseList()) {
			addBound(typeInfo, makeUseBound(regArg));
		}
	}

	private void mergePhiBounds(SSAVar ssaVar) {
		for (PhiInsn usedInPhi : ssaVar.getUsedInPhi()) {
			Set<ITypeBound> bounds = ssaVar.getTypeInfo().getBounds();
			bounds.addAll(usedInPhi.getResult().getSVar().getTypeInfo().getBounds());
			for (InsnArg arg : usedInPhi.getArguments()) {
				bounds.addAll(((RegisterArg) arg).getSVar().getTypeInfo().getBounds());
			}
		}
	}

	private void addBound(TypeInfo typeInfo, ITypeBound bound) {
		if (bound == null) {
			return;
		}
		if (bound instanceof ITypeBoundDynamic
				|| bound.getType() != ArgType.UNKNOWN) {
			typeInfo.getBounds().add(bound);
		}
	}

	private void addAssignBound(TypeInfo typeInfo, RegisterArg assign) {
		ArgType immutableType = assign.getImmutableType();
		if (immutableType != null) {
			addBound(typeInfo, new TypeBoundConst(BoundEnum.ASSIGN, immutableType));
			return;
		}
		InsnNode insn = assign.getParentInsn();
		if (insn == null || insn.getResult() == null) {
			addBound(typeInfo, new TypeBoundConst(BoundEnum.ASSIGN, assign.getInitType()));
			return;
		}
		switch (insn.getType()) {
			case NEW_INSTANCE:
				ArgType clsType = (ArgType) ((IndexInsnNode) insn).getIndex();
				addBound(typeInfo, new TypeBoundConst(BoundEnum.ASSIGN, clsType));
				break;

			case CONSTRUCTOR:
				ArgType ctrClsType = replaceAnonymousType((ConstructorInsn) insn);
				addBound(typeInfo, new TypeBoundConst(BoundEnum.ASSIGN, ctrClsType));
				break;

			case CONST:
				LiteralArg constLit = (LiteralArg) insn.getArg(0);
				addBound(typeInfo, new TypeBoundConst(BoundEnum.ASSIGN, constLit.getType()));
				break;

			case MOVE_EXCEPTION:
				ExcHandlerAttr excHandlerAttr = insn.get(AType.EXC_HANDLER);
				if (excHandlerAttr != null) {
					for (ClassInfo catchType : excHandlerAttr.getHandler().getCatchTypes()) {
						addBound(typeInfo, new TypeBoundConst(BoundEnum.ASSIGN, catchType.getType()));
					}
				} else {
					addBound(typeInfo, new TypeBoundConst(BoundEnum.ASSIGN, insn.getResult().getInitType()));
				}
				break;

			case INVOKE:
				addBound(typeInfo, makeAssignInvokeBound((InvokeNode) insn));
				break;

			case IGET:
				addBound(typeInfo, makeAssignFieldGetBound((IndexInsnNode) insn));
				break;

			case CHECK_CAST:
				addBound(typeInfo, new TypeBoundCheckCastAssign(root, (IndexInsnNode) insn));
				break;

			default:
				ArgType type = insn.getResult().getInitType();
				addBound(typeInfo, new TypeBoundConst(BoundEnum.ASSIGN, type));
				break;
		}
	}

	private ArgType replaceAnonymousType(ConstructorInsn ctr) {
		if (ctr.isNewInstance()) {
			ClassNode ctrCls = root.resolveClass(ctr.getClassType());
			if (ctrCls != null && ctrCls.contains(AFlag.DONT_GENERATE)) {
				AnonymousClassAttr baseTypeAttr = ctrCls.get(AType.ANONYMOUS_CLASS);
				if (baseTypeAttr != null && baseTypeAttr.getInlineType() == AnonymousClassAttr.InlineType.CONSTRUCTOR) {
					return baseTypeAttr.getBaseType();
				}
			}
		}
		return ctr.getClassType().getType();
	}

	private ITypeBound makeAssignFieldGetBound(IndexInsnNode insn) {
		ArgType initType = insn.getResult().getInitType();
		if (initType.containsTypeVariable()) {
			return new TypeBoundFieldGetAssign(root, insn, initType);
		}
		return new TypeBoundConst(BoundEnum.ASSIGN, initType);
	}

	private ITypeBound makeAssignInvokeBound(InvokeNode invokeNode) {
		ArgType boundType = invokeNode.getCallMth().getReturnType();
		ArgType genericReturnType = root.getMethodUtils().getMethodGenericReturnType(invokeNode);
		if (genericReturnType != null) {
			if (genericReturnType.containsTypeVariable()) {
				InvokeType invokeType = invokeNode.getInvokeType();
				if (invokeNode.getArgsCount() != 0
						&& invokeType != InvokeType.STATIC && invokeType != InvokeType.SUPER) {
					return new TypeBoundInvokeAssign(root, invokeNode, genericReturnType);
				}
			} else {
				boundType = genericReturnType;
			}
		}
		return new TypeBoundConst(BoundEnum.ASSIGN, boundType);
	}

	@Nullable
	private ITypeBound makeUseBound(RegisterArg regArg) {
		InsnNode insn = regArg.getParentInsn();
		if (insn == null) {
			return null;
		}
		if (insn instanceof BaseInvokeNode) {
			ITypeBound invokeUseBound = makeInvokeUseBound(regArg, (BaseInvokeNode) insn);
			if (invokeUseBound != null) {
				return invokeUseBound;
			}
		}
		if (insn.getType() == InsnType.CHECK_CAST && insn.contains(AFlag.SOFT_CAST)) {
			// ignore
			return null;
		}
		return new TypeBoundConst(BoundEnum.USE, regArg.getInitType(), regArg);
	}

	private ITypeBound makeInvokeUseBound(RegisterArg regArg, BaseInvokeNode invoke) {
		InsnArg instanceArg = invoke.getInstanceArg();
		if (instanceArg == null) {
			return null;
		}
		MethodUtils methodUtils = root.getMethodUtils();
		IMethodDetails methodDetails = methodUtils.getMethodDetails(invoke);
		if (methodDetails == null) {
			return null;
		}
		if (instanceArg != regArg) {
			int argIndex = invoke.getArgIndex(regArg) - invoke.getFirstArgOffset();
			ArgType argType = methodDetails.getArgTypes().get(argIndex);
			if (!argType.containsTypeVariable()) {
				return null;
			}
			return new TypeBoundInvokeUse(root, invoke, regArg, argType);
		}

		// for override methods use origin declared class as type
		if (methodDetails instanceof MethodNode) {
			MethodNode callMth = (MethodNode) methodDetails;
			ClassInfo declCls = methodUtils.getMethodOriginDeclClass(callMth);
			return new TypeBoundConst(BoundEnum.USE, declCls.getType(), regArg);
		}
		return null;
	}

	private boolean tryPossibleTypes(MethodNode mth, SSAVar var, ArgType type) {
		List<ArgType> types = makePossibleTypesList(type, var);
		if (types.isEmpty()) {
			return false;
		}
		for (ArgType candidateType : types) {
			TypeUpdateResult result = typeUpdate.apply(mth, var, candidateType);
			if (result == TypeUpdateResult.CHANGED) {
				return true;
			}
		}
		return false;
	}

	private List<ArgType> makePossibleTypesList(ArgType type, @Nullable SSAVar var) {
		if (type.isArray()) {
			List<ArgType> list = new ArrayList<>();
			for (ArgType arrElemType : makePossibleTypesList(type.getArrayElement(), null)) {
				list.add(ArgType.array(arrElemType));
			}
			return list;
		}
		if (var != null) {
			for (ITypeBound b : var.getTypeInfo().getBounds()) {
				ArgType boundType = b.getType();
				if (boundType.isObject() || boundType.isArray()) {
					// don't add primitive types
					return Collections.emptyList();
				}
			}
		}
		List<ArgType> list = new ArrayList<>();
		for (PrimitiveType possibleType : type.getPossibleTypes()) {
			if (possibleType == PrimitiveType.VOID) {
				continue;
			}
			list.add(ArgType.convertFromPrimitiveType(possibleType));
		}
		return list;
	}

	private boolean tryDeduceTypes(MethodNode mth) {
		boolean fixed = false;
		for (SSAVar ssaVar : mth.getSVars()) {
			if (deduceType(mth, ssaVar)) {
				fixed = true;
			}
		}
		return fixed;
	}

	private boolean deduceType(MethodNode mth, SSAVar var) {
		if (var.isTypeImmutable()) {
			return false;
		}
		ArgType type = var.getTypeInfo().getType();
		if (type.isTypeKnown()) {
			return false;
		}
		// try best type from bounds again
		if (setBestType(mth, var)) {
			return true;
		}
		// try all possible types (useful for primitives)
		if (tryPossibleTypes(mth, var, type)) {
			return true;
		}
		// for objects try super types
		if (tryWiderObjects(mth, var)) {
			return true;
		}
		return false;
	}

	private boolean tryRemoveGenerics(MethodNode mth) {
		boolean resolved = true;
		for (SSAVar var : mth.getSVars()) {
			ArgType type = var.getTypeInfo().getType();
			if (!type.isTypeKnown()
					&& !var.isTypeImmutable()
					&& !tryRawType(mth, var)) {
				resolved = false;
			}
		}
		return resolved;
	}

	private boolean tryRawType(MethodNode mth, SSAVar var) {
		Set<ArgType> objTypes = new LinkedHashSet<>();
		for (ITypeBound bound : var.getTypeInfo().getBounds()) {
			ArgType boundType = bound.getType();
			if (boundType.isTypeKnown() && boundType.isObject()) {
				objTypes.add(boundType);
			}
		}
		if (objTypes.isEmpty()) {
			return false;
		}
		for (ArgType objType : objTypes) {
			if (checkRawType(mth, var, objType)) {
				mth.addDebugComment("Type inference failed for " + var.toShortString() + "."
						+ " Raw type applied. Possible types: " + Utils.listToString(objTypes));
				return true;
			}
		}
		return false;
	}

	private boolean checkRawType(MethodNode mth, SSAVar var, ArgType objType) {
		if (objType.isObject() && objType.containsGeneric()) {
			ArgType rawType = objType.isGenericType() ? ArgType.OBJECT : ArgType.object(objType.getObject());
			TypeUpdateResult result = typeUpdate.applyWithWiderAllow(mth, var, rawType);
			return result == TypeUpdateResult.CHANGED;
		}
		return false;
	}

	/**
	 * Fix check casts to type var extend type:
	 * <br>
	 * {@code <T extends Comparable> T var = (Comparable) obj; => T var = (T) obj; }
	 */
	private boolean tryRestoreTypeVarCasts(MethodNode mth) {
		int changed = 0;
		List<SSAVar> mthSVars = mth.getSVars();
		for (SSAVar var : mthSVars) {
			changed += restoreTypeVarCasts(var);
		}
		if (changed == 0) {
			return false;
		}
		if (Consts.DEBUG_TYPE_INFERENCE) {
			mth.addDebugComment("Restore " + changed + " type vars casts");
		}
		initTypeBounds(mth);
		return runTypePropagation(mth);
	}

	private int restoreTypeVarCasts(SSAVar var) {
		TypeInfo typeInfo = var.getTypeInfo();
		Set<ITypeBound> bounds = typeInfo.getBounds();
		if (!ListUtils.anyMatch(bounds, t -> t.getType().isGenericType())) {
			return 0;
		}
		List<ITypeBound> casts = ListUtils.filter(bounds, TypeBoundCheckCastAssign.class::isInstance);
		if (casts.isEmpty()) {
			return 0;
		}
		ArgType bestType = selectBestTypeFromBounds(bounds).orElse(ArgType.UNKNOWN);
		if (!bestType.isGenericType()) {
			return 0;
		}
		List<ArgType> extendTypes = bestType.getExtendTypes();
		if (extendTypes.size() != 1) {
			return 0;
		}
		int fixed = 0;
		ArgType extendType = extendTypes.get(0);
		for (ITypeBound bound : casts) {
			TypeBoundCheckCastAssign cast = (TypeBoundCheckCastAssign) bound;
			ArgType castType = cast.getType();
			TypeCompareEnum result = typeUpdate.getTypeCompare().compareTypes(extendType, castType);
			if (result.isEqual() || result == TypeCompareEnum.NARROW_BY_GENERIC) {
				cast.getInsn().updateIndex(bestType);
				fixed++;
			}
		}
		return fixed;
	}

	@SuppressWarnings({ "ForLoopReplaceableByWhile", "ForLoopReplaceableByForEach" })
	private boolean tryInsertCasts(MethodNode mth) {
		int added = 0;
		List<SSAVar> mthSVars = mth.getSVars();
		int varsCount = mthSVars.size();
		for (int i = 0; i < varsCount; i++) {
			SSAVar var = mthSVars.get(i);
			ArgType type = var.getTypeInfo().getType();
			if (!type.isTypeKnown() && !var.isTypeImmutable()) {
				added += tryInsertVarCast(mth, var);
			}
		}
		if (added != 0) {
			if (Consts.DEBUG_TYPE_INFERENCE) {
				mth.addDebugComment("Additional " + added + " cast instructions added to help type inference");
			}
			InitCodeVariables.rerun(mth);
			initTypeBounds(mth);
			return runTypePropagation(mth);
		}
		return false;
	}

	private int tryInsertVarCast(MethodNode mth, SSAVar var) {
		for (ITypeBound bound : var.getTypeInfo().getBounds()) {
			ArgType boundType = bound.getType();
			if (boundType.isTypeKnown()
					&& !boundType.equals(var.getTypeInfo().getType())
					&& boundType.containsTypeVariable()
					&& !root.getTypeUtils().containsUnknownTypeVar(mth, boundType)) {
				if (insertAssignCast(mth, var, boundType)) {
					return 1;
				}
				return insertUseCasts(mth, var);
			}
		}
		return 0;
	}

	private int insertUseCasts(MethodNode mth, SSAVar var) {
		List<RegisterArg> useList = var.getUseList();
		if (useList.isEmpty()) {
			return 0;
		}
		int useCasts = 0;
		for (RegisterArg useReg : new ArrayList<>(useList)) {
			if (insertSoftUseCast(mth, useReg)) {
				useCasts++;
			}
		}
		return useCasts;
	}

	private boolean insertAssignCast(MethodNode mth, SSAVar var, ArgType castType) {
		RegisterArg assignArg = var.getAssign();
		InsnNode assignInsn = assignArg.getParentInsn();
		if (assignInsn == null || assignInsn.getType() == InsnType.PHI) {
			return false;
		}
		BlockNode assignBlock = BlockUtils.getBlockByInsn(mth, assignInsn);
		if (assignBlock == null) {
			return false;
		}
		assignInsn.setResult(assignArg.duplicateWithNewSSAVar(mth));
		IndexInsnNode castInsn = makeSoftCastInsn(assignArg.duplicate(), assignInsn.getResult().duplicate(), castType);
		return BlockUtils.insertAfterInsn(assignBlock, assignInsn, castInsn);
	}

	private boolean insertSoftUseCast(MethodNode mth, RegisterArg useArg) {
		InsnNode useInsn = useArg.getParentInsn();
		if (useInsn == null || useInsn.getType() == InsnType.PHI) {
			return false;
		}
		if (useInsn.getType() == InsnType.IF && useInsn.getArg(1).isZeroLiteral()) {
			// cast not needed if compare with null
			return false;
		}
		BlockNode useBlock = BlockUtils.getBlockByInsn(mth, useInsn);
		if (useBlock == null) {
			return false;
		}
		IndexInsnNode castInsn = makeSoftCastInsn(
				useArg.duplicateWithNewSSAVar(mth),
				useArg.duplicate(),
				useArg.getInitType());
		useInsn.replaceArg(useArg, castInsn.getResult().duplicate());
		return BlockUtils.insertBeforeInsn(useBlock, useInsn, castInsn);
	}

	@NotNull
	private IndexInsnNode makeSoftCastInsn(RegisterArg result, RegisterArg arg, ArgType castType) {
		IndexInsnNode castInsn = new IndexInsnNode(InsnType.CHECK_CAST, castType, 1);
		castInsn.setResult(result);
		castInsn.addArg(arg);
		castInsn.add(AFlag.SOFT_CAST);
		castInsn.add(AFlag.SYNTHETIC);
		return castInsn;
	}

	private boolean trySplitConstInsns(MethodNode mth) {
		boolean constSplitted = false;
		for (SSAVar var : new ArrayList<>(mth.getSVars())) {
			if (checkAndSplitConstInsn(mth, var)) {
				constSplitted = true;
			}
		}
		if (!constSplitted) {
			return false;
		}
		InitCodeVariables.rerun(mth);
		initTypeBounds(mth);
		return runTypePropagation(mth);
	}

	private boolean checkAndSplitConstInsn(MethodNode mth, SSAVar var) {
		ArgType type = var.getTypeInfo().getType();
		if (type.isTypeKnown() || var.isTypeImmutable()) {
			return false;
		}
		if (var.getUsedInPhi().size() < 2) {
			return false;
		}
		InsnNode assignInsn = var.getAssign().getAssignInsn();
		InsnNode constInsn = InsnUtils.checkInsnType(assignInsn, InsnType.CONST);
		if (constInsn == null) {
			return false;
		}
		BlockNode blockNode = BlockUtils.getBlockByInsn(mth, constInsn);
		if (blockNode == null) {
			return false;
		}
		// for every PHI make separate CONST insn
		boolean first = true;
		for (PhiInsn phiInsn : var.getUsedInPhi()) {
			if (first) {
				first = false;
				continue;
			}
			InsnNode copyInsn = constInsn.copyWithNewSsaVar(mth);
			copyInsn.add(AFlag.SYNTHETIC);
			BlockUtils.insertAfterInsn(blockNode, constInsn, copyInsn);

			RegisterArg phiArg = phiInsn.getArgBySsaVar(var);
			phiInsn.replaceArg(phiArg, copyInsn.getResult().duplicate());
		}
		return true;
	}

	private boolean tryInsertAdditionalMove(MethodNode mth) {
		int insnsAdded = 0;
		for (BlockNode block : mth.getBasicBlocks()) {
			PhiListAttr phiListAttr = block.get(AType.PHI_LIST);
			if (phiListAttr != null) {
				for (PhiInsn phiInsn : phiListAttr.getList()) {
					insnsAdded += tryInsertAdditionalInsn(mth, phiInsn);
				}
			}
		}
		if (insnsAdded == 0) {
			return false;
		}
		if (Consts.DEBUG_TYPE_INFERENCE) {
			mth.addDebugComment("Additional " + insnsAdded + " move instructions added to help type inference");
		}
		InitCodeVariables.rerun(mth);
		initTypeBounds(mth);
		if (runTypePropagation(mth) && checkTypes(mth)) {
			return true;
		}
		return tryDeduceTypes(mth);
	}

	/**
	 * Add MOVE instruction before PHI in bound blocks to make 'soft' type link.
	 * This allows to use different types in blocks merged by PHI.
	 */
	private int tryInsertAdditionalInsn(MethodNode mth, PhiInsn phiInsn) {
		ArgType phiType = getCommonTypeForPhiArgs(phiInsn);
		if (phiType != null && phiType.isTypeKnown()) {
			// all args have same known type => nothing to do here
			return 0;
		}
		// check if instructions can be inserted
		if (insertMovesForPhi(mth, phiInsn, false) == 0) {
			return 0;
		}
		// check passed => apply
		return insertMovesForPhi(mth, phiInsn, true);
	}

	@Nullable
	private ArgType getCommonTypeForPhiArgs(PhiInsn phiInsn) {
		ArgType phiArgType = null;
		for (InsnArg arg : phiInsn.getArguments()) {
			ArgType type = arg.getType();
			if (phiArgType == null) {
				phiArgType = type;
			} else if (!phiArgType.equals(type)) {
				return null;
			}
		}
		return phiArgType;
	}

	private int insertMovesForPhi(MethodNode mth, PhiInsn phiInsn, boolean apply) {
		int argsCount = phiInsn.getArgsCount();
		int count = 0;
		for (int argIndex = 0; argIndex < argsCount; argIndex++) {
			RegisterArg reg = phiInsn.getArg(argIndex);
			BlockNode startBlock = phiInsn.getBlockByArgIndex(argIndex);
			BlockNode blockNode = checkBlockForInsnInsert(startBlock);
			if (blockNode == null) {
				mth.addDebugComment("Failed to insert an additional move for type inference into block " + startBlock);
				return 0;
			}
			boolean add = true;
			SSAVar var = reg.getSVar();
			InsnNode assignInsn = var.getAssign().getAssignInsn();
			if (assignInsn != null) {
				InsnType assignType = assignInsn.getType();
				if (assignType == InsnType.CONST
						|| (assignType == InsnType.MOVE && var.getUseCount() == 1)) {
					add = false;
				}
			}
			if (add) {
				count++;
				if (apply) {
					insertMove(mth, blockNode, phiInsn, reg);
				}
			}
		}
		return count;
	}

	private void insertMove(MethodNode mth, BlockNode blockNode, PhiInsn phiInsn, RegisterArg reg) {
		SSAVar var = reg.getSVar();
		int regNum = reg.getRegNum();
		RegisterArg resultArg = reg.duplicate(regNum, null);
		SSAVar newSsaVar = mth.makeNewSVar(resultArg);
		RegisterArg arg = reg.duplicate(regNum, var);

		InsnNode moveInsn = new InsnNode(InsnType.MOVE, 1);
		moveInsn.setResult(resultArg);
		moveInsn.addArg(arg);
		moveInsn.add(AFlag.SYNTHETIC);
		blockNode.getInstructions().add(moveInsn);

		phiInsn.replaceArg(reg, reg.duplicate(regNum, newSsaVar));
	}

	@Nullable
	private BlockNode checkBlockForInsnInsert(BlockNode blockNode) {
		if (blockNode.isSynthetic()) {
			return null;
		}
		InsnNode lastInsn = BlockUtils.getLastInsn(blockNode);
		if (lastInsn != null && BlockSplitter.isSeparate(lastInsn.getType())) {
			// can't insert move in a block with 'separate' instruction => try previous block by simple path
			List<BlockNode> preds = blockNode.getPredecessors();
			if (preds.size() == 1) {
				return checkBlockForInsnInsert(preds.get(0));
			}
			return null;
		}
		return blockNode;
	}

	private boolean tryWiderObjects(MethodNode mth, SSAVar var) {
		Set<ArgType> objTypes = new LinkedHashSet<>();
		for (ITypeBound bound : var.getTypeInfo().getBounds()) {
			ArgType boundType = bound.getType();
			if (boundType.isTypeKnown() && boundType.isObject()) {
				objTypes.add(boundType);
			}
		}
		if (objTypes.isEmpty()) {
			return false;
		}
		ClspGraph clsp = mth.root().getClsp();
		for (ArgType objType : objTypes) {
			for (String ancestor : clsp.getSuperTypes(objType.getObject())) {
				ArgType ancestorType = ArgType.object(ancestor);
				TypeUpdateResult result = typeUpdate.applyWithWiderAllow(mth, var, ancestorType);
				if (result == TypeUpdateResult.CHANGED) {
					return true;
				}
			}
		}
		return false;
	}

	@SuppressWarnings("ForLoopReplaceableByForEach")
	private boolean tryToFixIncompatiblePrimitives(MethodNode mth) {
		boolean fixed = false;
		List<SSAVar> ssaVars = mth.getSVars();
		int ssaVarsCount = ssaVars.size();
		// new vars will be added at list end if fix is applied (can't use for-each loop)
		for (int i = 0; i < ssaVarsCount; i++) {
			if (processIncompatiblePrimitives(mth, ssaVars.get(i))) {
				fixed = true;
			}
		}
		if (!fixed) {
			return false;
		}
		InitCodeVariables.rerun(mth);
		initTypeBounds(mth);
		return runTypePropagation(mth);
	}

	private boolean processIncompatiblePrimitives(MethodNode mth, SSAVar var) {
		TypeInfo typeInfo = var.getTypeInfo();
		if (typeInfo.getType().isTypeKnown()) {
			return false;
		}
		boolean assigned = false;
		for (ITypeBound bound : typeInfo.getBounds()) {
			ArgType boundType = bound.getType();
			switch (bound.getBound()) {
				case ASSIGN:
					if (!boundType.contains(PrimitiveType.BOOLEAN)) {
						return false;
					}
					assigned = true;
					break;
				case USE:
					if (!boundType.canBeAnyNumber()) {
						return false;
					}
					break;
			}
		}
		if (!assigned) {
			return false;
		}

		boolean fixed = false;
		for (RegisterArg arg : new ArrayList<>(var.getUseList())) {
			if (fixBooleanUsage(mth, arg)) {
				fixed = true;
			}
		}
		return fixed;
	}

	private boolean fixBooleanUsage(MethodNode mth, RegisterArg boundArg) {
		ArgType boundType = boundArg.getInitType();
		if (boundType == ArgType.BOOLEAN
				|| (boundType.isTypeKnown() && !boundType.isPrimitive())) {
			return false;
		}
		InsnNode insn = boundArg.getParentInsn();
		if (insn == null || insn.getType() == InsnType.IF) {
			return false;
		}
		BlockNode blockNode = BlockUtils.getBlockByInsn(mth, insn);
		if (blockNode == null) {
			return false;
		}
		List<InsnNode> insnList = blockNode.getInstructions();
		int insnIndex = InsnList.getIndex(insnList, insn);
		if (insnIndex == -1) {
			return false;
		}
		InsnType insnType = insn.getType();
		if (insnType == InsnType.CAST) {
			// replace cast
			ArgType type = (ArgType) ((IndexInsnNode) insn).getIndex();
			TernaryInsn convertInsn = prepareBooleanConvertInsn(insn.getResult(), boundArg, type);
			BlockUtils.replaceInsn(mth, blockNode, insnIndex, convertInsn);
			return true;
		}
		if (insnType == InsnType.ARITH) {
			ArithNode arithInsn = (ArithNode) insn;
			if (arithInsn.getOp() == ArithOp.XOR && arithInsn.getArgsCount() == 2) {
				// replace (boolean ^ 1) with (!boolean)
				InsnArg secondArg = arithInsn.getArg(1);
				if (secondArg.isLiteral() && ((LiteralArg) secondArg).getLiteral() == 1) {
					InsnNode convertInsn = notBooleanToInt(arithInsn, boundArg);
					BlockUtils.replaceInsn(mth, blockNode, insnIndex, convertInsn);
					return true;
				}
			}
		}

		// insert before insn
		RegisterArg resultArg = boundArg.duplicateWithNewSSAVar(mth);
		TernaryInsn convertInsn = prepareBooleanConvertInsn(resultArg, boundArg, boundType);
		insnList.add(insnIndex, convertInsn);
		insn.replaceArg(boundArg, convertInsn.getResult().duplicate());
		return true;
	}

	private InsnNode notBooleanToInt(ArithNode insn, RegisterArg boundArg) {
		InsnNode notInsn = new InsnNode(InsnType.NOT, 1);
		notInsn.addArg(boundArg.duplicate());
		notInsn.add(AFlag.SYNTHETIC);

		InsnArg notArg = InsnArg.wrapArg(notInsn);
		notArg.setType(ArgType.BOOLEAN);
		TernaryInsn convertInsn = ModVisitor.makeBooleanConvertInsn(insn.getResult(), notArg, ArgType.INT);
		convertInsn.add(AFlag.SYNTHETIC);
		return convertInsn;
	}

	private TernaryInsn prepareBooleanConvertInsn(RegisterArg resultArg, RegisterArg boundArg, ArgType useType) {
		RegisterArg useArg = boundArg.getSVar().getAssign().duplicate();
		TernaryInsn convertInsn = ModVisitor.makeBooleanConvertInsn(resultArg, useArg, useType);
		convertInsn.add(AFlag.SYNTHETIC);
		return convertInsn;
	}

	private boolean tryToForceImmutableTypes(MethodNode mth) {
		boolean fixed = false;
		for (SSAVar ssaVar : mth.getSVars()) {
			ArgType type = ssaVar.getTypeInfo().getType();
			if (!type.isTypeKnown() && ssaVar.isTypeImmutable()) {
				if (forceImmutableType(ssaVar)) {
					fixed = true;
				}
			}
		}
		if (!fixed) {
			return false;
		}
		return runTypePropagation(mth);
	}

	private boolean forceImmutableType(SSAVar ssaVar) {
		for (RegisterArg useArg : ssaVar.getUseList()) {
			InsnNode parentInsn = useArg.getParentInsn();
			if (parentInsn != null) {
				InsnType insnType = parentInsn.getType();
				if (insnType == InsnType.AGET || insnType == InsnType.APUT) {
					ssaVar.setType(ssaVar.getImmutableType());
					return true;
				}
			}
		}
		return false;
	}

	private static void assignImmutableTypes(MethodNode mth) {
		for (SSAVar ssaVar : mth.getSVars()) {
			ArgType immutableType = getSsaImmutableType(ssaVar);
			if (immutableType != null) {
				ssaVar.markAsImmutable(immutableType);
			}
		}
	}

	@Nullable
	private static ArgType getSsaImmutableType(SSAVar ssaVar) {
		if (ssaVar.getAssign().contains(AFlag.IMMUTABLE_TYPE)) {
			return ssaVar.getAssign().getInitType();
		}
		for (RegisterArg reg : ssaVar.getUseList()) {
			if (reg.contains(AFlag.IMMUTABLE_TYPE)) {
				return reg.getInitType();
			}
		}
		return null;
	}
}
