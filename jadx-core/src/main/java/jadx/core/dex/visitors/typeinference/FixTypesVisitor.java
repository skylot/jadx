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

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.Consts;
import jadx.core.clsp.ClspGraph;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.PhiListAttr;
import jadx.core.dex.instructions.ArithNode;
import jadx.core.dex.instructions.ArithOp;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.PrimitiveType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.instructions.mods.TernaryInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.InitCodeVariables;
import jadx.core.dex.visitors.JadxVisitor;
import jadx.core.dex.visitors.ModVisitor;
import jadx.core.dex.visitors.blocks.BlockSplitter;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InsnList;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.ListUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxOverflowException;

@JadxVisitor(
		name = "Fix Types Visitor",
		desc = "Try various methods to fix unresolved types",
		runAfter = {
				TypeInferenceVisitor.class
		},
		runBefore = {
				FinishTypeInference.class
		}
)
public final class FixTypesVisitor extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(FixTypesVisitor.class);

	private final TypeInferenceVisitor typeInference = new TypeInferenceVisitor();

	private TypeUpdate typeUpdate;
	private List<Function<MethodNode, Boolean>> resolvers;

	@Override
	public void init(RootNode root) {
		this.typeUpdate = root.getTypeUpdate();
		this.typeInference.init(root);
		this.resolvers = Arrays.asList(
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
		if (mth.isNoCode() || checkTypes(mth)) {
			return;
		}
		try {
			for (Function<MethodNode, Boolean> resolver : resolvers) {
				if (resolver.apply(mth) && checkTypes(mth)) {
					break;
				}
			}
		} catch (Exception e) {
			mth.addError("Types fix failed", e);
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

	private boolean setBestType(MethodNode mth, SSAVar ssaVar) {
		try {
			return calculateFromBounds(mth, ssaVar);
		} catch (JadxOverflowException e) {
			throw e;
		} catch (Exception e) {
			mth.addWarnComment("Failed to calculate best type for var: " + ssaVar, e);
			return false;
		}
	}

	private boolean calculateFromBounds(MethodNode mth, SSAVar ssaVar) {
		TypeInfo typeInfo = ssaVar.getTypeInfo();
		Set<ITypeBound> bounds = typeInfo.getBounds();
		Optional<ArgType> bestTypeOpt = selectBestTypeFromBounds(bounds);
		if (bestTypeOpt.isEmpty()) {
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

	@SuppressWarnings("RedundantIfStatement")
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
		typeInference.initTypeBounds(mth);
		return typeInference.runTypePropagation(mth);
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
			InitCodeVariables.rerun(mth);
			typeInference.initTypeBounds(mth);
			return typeInference.runTypePropagation(mth);
		}
		return false;
	}

	private int tryInsertVarCast(MethodNode mth, SSAVar var) {
		for (ITypeBound bound : var.getTypeInfo().getBounds()) {
			ArgType boundType = bound.getType();
			if (boundType.isTypeKnown()
					&& !boundType.equals(var.getTypeInfo().getType())
					&& boundType.containsTypeVariable()
					&& !mth.root().getTypeUtils().containsUnknownTypeVar(mth, boundType)) {
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
		if (useInsn.getType() == InsnType.IF && useInsn.getArg(1).isZeroConst()) {
			// cast isn't needed if compare with null
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
		boolean success = BlockUtils.insertBeforeInsn(useBlock, useInsn, castInsn);
		if (Consts.DEBUG_TYPE_INFERENCE && success) {
			LOG.info("Insert soft cast for {} before {} in {}", useArg, useInsn, useBlock);
		}
		return success;
	}

	private IndexInsnNode makeSoftCastInsn(RegisterArg result, RegisterArg arg, ArgType castType) {
		IndexInsnNode castInsn = new IndexInsnNode(InsnType.CHECK_CAST, castType, 1);
		castInsn.setResult(result);
		castInsn.addArg(arg);
		castInsn.add(AFlag.SOFT_CAST);
		castInsn.add(AFlag.SYNTHETIC);
		return castInsn;
	}

	private boolean trySplitConstInsns(MethodNode mth) {
		boolean constSplit = false;
		for (SSAVar var : new ArrayList<>(mth.getSVars())) {
			if (checkAndSplitConstInsn(mth, var)) {
				constSplit = true;
			}
		}
		if (!constSplit) {
			return false;
		}
		InitCodeVariables.rerun(mth);
		typeInference.initTypeBounds(mth);
		return typeInference.runTypePropagation(mth);
	}

	private boolean checkAndSplitConstInsn(MethodNode mth, SSAVar var) {
		ArgType type = var.getTypeInfo().getType();
		if (type.isTypeKnown() || var.isTypeImmutable()) {
			return false;
		}
		return splitByPhi(mth, var) || dupConst(mth, var);
	}

	private boolean dupConst(MethodNode mth, SSAVar var) {
		InsnNode assignInsn = var.getAssign().getAssignInsn();
		if (!InsnUtils.isInsnType(assignInsn, InsnType.CONST)) {
			return false;
		}
		if (var.getUseList().size() < 2) {
			return false;
		}
		BlockNode assignBlock = BlockUtils.getBlockByInsn(mth, assignInsn);
		if (assignBlock == null) {
			return false;
		}
		assignInsn.remove(AFlag.DONT_INLINE);
		int insertIndex = 1 + BlockUtils.getInsnIndexInBlock(assignBlock, assignInsn);
		List<RegisterArg> useList = new ArrayList<>(var.getUseList());
		for (int i = 0, useCount = useList.size(); i < useCount; i++) {
			RegisterArg useArg = useList.get(i);
			useArg.remove(AFlag.DONT_INLINE_CONST);
			if (i == 0) {
				continue;
			}
			InsnNode useInsn = useArg.getParentInsn();
			if (useInsn == null) {
				continue;
			}
			InsnNode newInsn = assignInsn.copyWithNewSsaVar(mth);
			assignBlock.getInstructions().add(insertIndex, newInsn);
			useInsn.replaceArg(useArg, newInsn.getResult().duplicate());
		}
		if (Consts.DEBUG_TYPE_INFERENCE) {
			LOG.debug("Duplicate const insn {} times: {} in {}", useList.size(), assignInsn, assignBlock);
		}
		return true;
	}

	/**
	 * For every PHI make separate CONST insn
	 */
	private static boolean splitByPhi(MethodNode mth, SSAVar var) {
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
		typeInference.initTypeBounds(mth);
		if (typeInference.runTypePropagation(mth) && checkTypes(mth)) {
			return true;
		}
		return tryDeduceTypes(mth);
	}

	/**
	 * Add MOVE instruction before PHI in bound blocks to make 'soft' type link.
	 * This allows using different types in blocks merged by PHI.
	 */
	private int tryInsertAdditionalInsn(MethodNode mth, PhiInsn phiInsn) {
		ArgType phiType = getCommonTypeForPhiArgs(phiInsn);
		if (phiType != null && phiType.isTypeKnown()) {
			// all args have the same known type => nothing to do here
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
		// new vars will be added at a list end if fix is applied (can't use for-each loop here)
		for (int i = 0; i < ssaVarsCount; i++) {
			if (processIncompatiblePrimitives(mth, ssaVars.get(i))) {
				fixed = true;
			}
		}
		if (!fixed) {
			return false;
		}
		InitCodeVariables.rerun(mth);
		typeInference.initTypeBounds(mth);
		return typeInference.runTypePropagation(mth);
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
				if (Consts.DEBUG_TYPE_INFERENCE) {
					LOG.info("Fixed boolean usage for arg {} from {}", arg, arg.getParentInsn());
				}
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

		ArgType resType = insn.getResult().getType();
		if (resType.canBePrimitive(PrimitiveType.BOOLEAN)) {
			notInsn.setResult(insn.getResult());
			return notInsn;
		}
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
		return typeInference.runTypePropagation(mth);
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

	@Override
	public String getName() {
		return "FixTypesVisitor";
	}
}
