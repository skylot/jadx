package jadx.core.dex.visitors.typeinference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.Consts;
import jadx.core.clsp.ClspGraph;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.info.ClassInfo;
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
import jadx.core.dex.instructions.mods.TernaryInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IMethodDetails;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.trycatch.ExcHandlerAttr;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.AttachMethodDetails;
import jadx.core.dex.visitors.ConstInlineVisitor;
import jadx.core.dex.visitors.InitCodeVariables;
import jadx.core.dex.visitors.JadxVisitor;
import jadx.core.dex.visitors.ModVisitor;
import jadx.core.dex.visitors.blocksmaker.BlockSplitter;
import jadx.core.dex.visitors.ssa.SSATransform;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InsnList;
import jadx.core.utils.InsnUtils;
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
				this::runTypePropagation,
				this::tryDeduceTypes,
				this::trySplitConstInsns,
				this::tryToFixIncompatiblePrimitives,
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
	private boolean checkTypes(MethodNode mth) {
		for (SSAVar var : mth.getSVars()) {
			ArgType type = var.getTypeInfo().getType();
			if (!type.isTypeKnown()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Guess type from usage and try to set it to current variable
	 * and all connected instructions with {@link TypeUpdate#apply(MethodNode, SSAVar, ArgType)}
	 */
	private boolean runTypePropagation(MethodNode mth) {
		List<SSAVar> ssaVars = mth.getSVars();
		// collect initial type bounds from assign and usages`
		ssaVars.forEach(this::attachBounds);
		ssaVars.forEach(this::mergePhiBounds);

		// start initial type propagation
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
				applyImmutableType(mth, ssaVar, immutableType);
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

	private void applyImmutableType(MethodNode mth, SSAVar ssaVar, ArgType initType) {
		TypeUpdateResult result = typeUpdate.apply(mth, ssaVar, initType);
		if (Consts.DEBUG_TYPE_INFERENCE && result == TypeUpdateResult.REJECT) {
			LOG.info("Reject initial immutable type {} for {}", initType, ssaVar);
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
					LOG.debug("Type set rejected: {} -> {}, bounds: {}", ssaVar, candidateType, bounds);
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

			default:
				ArgType type = insn.getResult().getInitType();
				addBound(typeInfo, new TypeBoundConst(BoundEnum.ASSIGN, type));
				break;
		}
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
			TypeBoundInvokeUse invokeUseBound = makeInvokeUseBound(regArg, (BaseInvokeNode) insn);
			if (invokeUseBound != null) {
				return invokeUseBound;
			}
		}
		return new TypeBoundConst(BoundEnum.USE, regArg.getInitType(), regArg);
	}

	private TypeBoundInvokeUse makeInvokeUseBound(RegisterArg regArg, BaseInvokeNode invoke) {
		InsnArg instanceArg = invoke.getInstanceArg();
		if (instanceArg == null || instanceArg == regArg) {
			return null;
		}
		IMethodDetails methodDetails = root.getMethodUtils().getMethodDetails(invoke);
		if (methodDetails == null) {
			return null;
		}
		int argIndex = invoke.getArgIndex(regArg) - invoke.getFirstArgOffset();
		ArgType argType = methodDetails.getArgTypes().get(argIndex);
		if (!argType.containsTypeVariable()) {
			return null;
		}
		return new TypeBoundInvokeUse(root, invoke, regArg, argType);
	}

	private boolean tryPossibleTypes(MethodNode mth, SSAVar var, ArgType type) {
		List<ArgType> types = makePossibleTypesList(type);
		for (ArgType candidateType : types) {
			TypeUpdateResult result = typeUpdate.apply(mth, var, candidateType);
			if (result == TypeUpdateResult.CHANGED) {
				return true;
			}
		}
		return false;
	}

	private List<ArgType> makePossibleTypesList(ArgType type) {
		List<ArgType> list = new ArrayList<>();
		if (type.isArray()) {
			for (ArgType arrElemType : makePossibleTypesList(type.getArrayElement())) {
				list.add(ArgType.array(arrElemType));
			}
		}
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
			ArgType rawType = ArgType.object(objType.getObject());
			TypeUpdateResult result = typeUpdate.applyWithWiderAllow(mth, var, rawType);
			return result == TypeUpdateResult.CHANGED;
		}
		return false;
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
		return runTypePropagation(mth);
	}

	private boolean checkAndSplitConstInsn(MethodNode mth, SSAVar var) {
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
		for (SSAVar var : new ArrayList<>(mth.getSVars())) {
			insnsAdded += tryInsertAdditionalInsn(mth, var);
		}
		if (insnsAdded == 0) {
			return false;
		}
		mth.addDebugComment("Additional " + insnsAdded + " move instruction added to help type inference");

		InitCodeVariables.rerun(mth);
		if (runTypePropagation(mth) && checkTypes(mth)) {
			return true;
		}
		return tryDeduceTypes(mth);
	}

	/**
	 * Add MOVE instruction before PHI in bound blocks to make 'soft' type link.
	 * This allows to use different types in blocks merged by PHI.
	 */
	private int tryInsertAdditionalInsn(MethodNode mth, SSAVar var) {
		if (var.getTypeInfo().getType().isTypeKnown()) {
			return 0;
		}
		List<PhiInsn> usedInPhiList = var.getUsedInPhi();
		if (usedInPhiList.isEmpty()) {
			return 0;
		}
		InsnNode assignInsn = var.getAssign().getAssignInsn();
		if (assignInsn != null) {
			InsnType assignType = assignInsn.getType();
			if (assignType == InsnType.CONST) {
				return 0;
			}
			if (assignType == InsnType.MOVE && var.getUseCount() == 1) {
				return 0;
			}
		}
		for (PhiInsn phiInsn : usedInPhiList) {
			if (!insertMoveForPhi(mth, phiInsn, var, false)) {
				return 0;
			}
		}

		// all check passed => apply
		for (PhiInsn phiInsn : usedInPhiList) {
			insertMoveForPhi(mth, phiInsn, var, true);
		}
		return usedInPhiList.size();
	}

	private boolean insertMoveForPhi(MethodNode mth, PhiInsn phiInsn, SSAVar var, boolean apply) {
		int argsCount = phiInsn.getArgsCount();
		for (int argIndex = 0; argIndex < argsCount; argIndex++) {
			RegisterArg reg = phiInsn.getArg(argIndex);
			if (reg.getSVar() == var) {
				BlockNode startBlock = phiInsn.getBlockByArgIndex(argIndex);
				BlockNode blockNode = checkBlockForInsnInsert(startBlock);
				if (blockNode == null) {
					mth.addWarnComment("Failed to insert an additional move for type inference into block " + startBlock);
					return false;
				}
				if (apply) {
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
				return true;
			}
		}
		return false;
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

	private boolean tryToFixIncompatiblePrimitives(MethodNode mth) {
		boolean fixed = false;
		for (SSAVar var : new ArrayList<>(mth.getSVars())) {
			if (processIncompatiblePrimitives(mth, var)) {
				fixed = true;
			}
		}
		if (!fixed) {
			return false;
		}
		InitCodeVariables.rerun(mth);
		return runTypePropagation(mth);
	}

	private boolean processIncompatiblePrimitives(MethodNode mth, SSAVar var) {
		TypeInfo typeInfo = var.getTypeInfo();
		if (typeInfo.getType().isTypeKnown()) {
			return false;
		}
		boolean boolAssign = false;
		for (ITypeBound bound : typeInfo.getBounds()) {
			if (bound.getBound() == BoundEnum.ASSIGN && bound.getType().equals(ArgType.BOOLEAN)) {
				boolAssign = true;
				break;
			}
		}
		if (!boolAssign) {
			return false;
		}

		boolean fixed = false;
		for (ITypeBound bound : typeInfo.getBounds()) {
			if (bound.getBound() == BoundEnum.USE
					&& fixBooleanUsage(mth, bound)) {
				fixed = true;
			}
		}
		return fixed;
	}

	private boolean fixBooleanUsage(MethodNode mth, ITypeBound bound) {
		ArgType boundType = bound.getType();
		if (!boundType.isPrimitive() || boundType == ArgType.BOOLEAN) {
			return false;
		}
		RegisterArg boundArg = bound.getArg();
		if (boundArg == null) {
			return false;
		}
		InsnNode insn = boundArg.getParentInsn();
		if (insn == null) {
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
		if (insn.getType() == InsnType.CAST) {
			// replace cast
			ArgType type = (ArgType) ((IndexInsnNode) insn).getIndex();
			TernaryInsn convertInsn = prepareBooleanConvertInsn(insn.getResult(), boundArg, type);
			BlockUtils.replaceInsn(mth, blockNode, insnIndex, convertInsn);
		} else {
			// insert before insn
			RegisterArg resultArg = boundArg.duplicateWithNewSSAVar(mth);
			TernaryInsn convertInsn = prepareBooleanConvertInsn(resultArg, boundArg, boundType);
			insnList.add(insnIndex, convertInsn);
			insn.replaceArg(bound.getArg(), convertInsn.getResult().duplicate());
		}
		return true;
	}

	private TernaryInsn prepareBooleanConvertInsn(RegisterArg resultArg, RegisterArg boundArg, ArgType useType) {
		RegisterArg useArg = boundArg.getSVar().getAssign().duplicate();
		TernaryInsn convertInsn = ModVisitor.makeBooleanConvertInsn(resultArg, useArg, useType);
		convertInsn.add(AFlag.SYNTHETIC);
		return convertInsn;
	}
}
