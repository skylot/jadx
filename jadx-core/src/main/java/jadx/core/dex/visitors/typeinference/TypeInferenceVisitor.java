package jadx.core.dex.visitors.typeinference;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.Consts;
import jadx.core.clsp.ClspGraph;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.InvokeType;
import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.CodeVar;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.PrimitiveType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.trycatch.ExcHandlerAttr;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.ConstInlineVisitor;
import jadx.core.dex.visitors.InitCodeVariables;
import jadx.core.dex.visitors.JadxVisitor;
import jadx.core.dex.visitors.blocksmaker.BlockSplitter;
import jadx.core.dex.visitors.ssa.SSATransform;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.Utils;

@JadxVisitor(
		name = "Type Inference",
		desc = "Calculate best types for SSA variables",
		runAfter = {
				SSATransform.class,
				ConstInlineVisitor.class
		}
)
public final class TypeInferenceVisitor extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(TypeInferenceVisitor.class);

	private RootNode root;
	private TypeUpdate typeUpdate;

	@Override
	public void init(RootNode root) {
		this.root = root;
		this.typeUpdate = root.getTypeUpdate();
	}

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}
		boolean resolved = runTypePropagation(mth);
		if (!resolved) {
			boolean moveAdded = false;
			for (SSAVar var : new ArrayList<>(mth.getSVars())) {
				moveAdded |= tryInsertAdditionalInsn(mth, var);
			}
			if (moveAdded) {
				InitCodeVariables.rerun(mth);
				resolved = runTypePropagation(mth);
			}
			if (!resolved) {
				resolved = runMultiVariableSearch(mth);
			}
		}
		if (resolved) {
			for (SSAVar var : new ArrayList<>(mth.getSVars())) {
				processIncompatiblePrimitives(mth, var);
			}
		}
	}

	/**
	 * Guess type from usage and try to set it to current variable
	 * and all connected instructions with {@link TypeUpdate#apply(SSAVar, ArgType)}
	 */
	private boolean runTypePropagation(MethodNode mth) {
		// collect initial type bounds from assign and usages`
		mth.getSVars().forEach(this::attachBounds);
		mth.getSVars().forEach(this::mergePhiBounds);

		// start initial type propagation
		mth.getSVars().forEach(this::setImmutableType);
		mth.getSVars().forEach(this::setBestType);

		// try other types if type is still unknown
		boolean resolved = true;
		for (SSAVar var : mth.getSVars()) {
			ArgType type = var.getTypeInfo().getType();
			if (!type.isTypeKnown()
					&& !var.isTypeImmutable()
					&& !tryDeduceType(mth, var, type)) {
				resolved = false;
			}
		}
		return resolved;
	}

	private boolean runMultiVariableSearch(MethodNode mth) {
		TypeSearch typeSearch = new TypeSearch(mth);
		try {
			boolean success = typeSearch.run();
			if (!success) {
				mth.addWarn("Multi-variable type inference failed");
			}
			return success;
		} catch (Exception e) {
			mth.addWarn("Multi-variable type inference failed. Error: " + Utils.getStackTrace(e));
			return false;
		}
	}

	private boolean setImmutableType(SSAVar ssaVar) {
		try {
			ArgType immutableType = ssaVar.getImmutableType();
			if (immutableType != null) {
				return applyImmutableType(ssaVar, immutableType);
			}
			return false;
		} catch (Exception e) {
			LOG.error("Failed to set immutable type for var: {}", ssaVar, e);
			return false;
		}
	}

	private boolean setBestType(SSAVar ssaVar) {
		try {
			return calculateFromBounds(ssaVar);
		} catch (Exception e) {
			LOG.error("Failed to calculate best type for var: {}", ssaVar, e);
			return false;
		}
	}

	private boolean applyImmutableType(SSAVar ssaVar, ArgType initType) {
		TypeUpdateResult result = typeUpdate.apply(ssaVar, initType);
		if (result == TypeUpdateResult.REJECT) {
			if (Consts.DEBUG) {
				LOG.info("Reject initial immutable type {} for {}", initType, ssaVar);
			}
			return false;
		}
		return result == TypeUpdateResult.CHANGED;
	}

	private boolean calculateFromBounds(SSAVar ssaVar) {
		TypeInfo typeInfo = ssaVar.getTypeInfo();
		Set<ITypeBound> bounds = typeInfo.getBounds();
		Optional<ArgType> bestTypeOpt = selectBestTypeFromBounds(bounds);
		if (!bestTypeOpt.isPresent()) {
			if (Consts.DEBUG) {
				LOG.warn("Failed to select best type from bounds, count={} : ", bounds.size());
				for (ITypeBound bound : bounds) {
					LOG.warn("  {}", bound);
				}
			}
			return false;
		}
		ArgType candidateType = bestTypeOpt.get();
		TypeUpdateResult result = typeUpdate.apply(ssaVar, candidateType);
		if (result == TypeUpdateResult.REJECT) {
			if (Consts.DEBUG) {
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
		if (bound != null && bound.getType() != ArgType.UNKNOWN) {
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
		MethodInfo callMth = invokeNode.getCallMth();
		ArgType boundType = callMth.getReturnType();
		ArgType genericReturnType = root.getMethodGenericReturnType(callMth);
		if (genericReturnType != null) {
			if (genericReturnType.containsGenericType()) {
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
		return new TypeBoundConst(BoundEnum.USE, regArg.getInitType(), regArg);
	}

	private boolean tryPossibleTypes(SSAVar var, ArgType type) {
		List<ArgType> types = makePossibleTypesList(type);
		for (ArgType candidateType : types) {
			TypeUpdateResult result = typeUpdate.apply(var, candidateType);
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

	private boolean tryDeduceType(MethodNode mth, SSAVar var, @Nullable ArgType type) {
		// try best type from bounds again
		if (setBestType(var)) {
			return true;
		}
		// try all possible types (useful for primitives)
		if (type != null && tryPossibleTypes(var, type)) {
			return true;
		}
		// for objects try super types
		if (tryWiderObjects(mth, var)) {
			return true;
		}
		return false;
	}

	/**
	 * Add MOVE instruction before PHI in bound blocks to make 'soft' type link.
	 * This allows to use different types in blocks merged by PHI.
	 */
	private boolean tryInsertAdditionalInsn(MethodNode mth, SSAVar var) {
		if (var.getTypeInfo().getType().isTypeKnown()) {
			return false;
		}
		List<PhiInsn> usedInPhiList = var.getUsedInPhi();
		if (usedInPhiList.isEmpty()) {
			return false;
		}
		InsnNode assignInsn = var.getAssign().getAssignInsn();
		if (assignInsn != null) {
			InsnType assignType = assignInsn.getType();
			if (assignType == InsnType.CONST) {
				return false;
			}
			if (assignType == InsnType.MOVE && var.getUseCount() == 1) {
				return false;
			}
		}
		for (PhiInsn phiInsn : usedInPhiList) {
			if (!insertMoveForPhi(mth, phiInsn, var)) {
				return false;
			}
		}
		return true;
	}

	private boolean insertMoveForPhi(MethodNode mth, PhiInsn phiInsn, SSAVar var) {
		int argsCount = phiInsn.getArgsCount();
		for (int argIndex = 0; argIndex < argsCount; argIndex++) {
			RegisterArg reg = phiInsn.getArg(argIndex);
			if (reg.getSVar() == var) {
				BlockNode blockNode = phiInsn.getBlockByArgIndex(argIndex);
				InsnNode lastInsn = BlockUtils.getLastInsn(blockNode);
				if (lastInsn != null && BlockSplitter.isSeparate(lastInsn.getType())) {
					// can't insert move in block with separate instruction
					// trying previous block
					List<BlockNode> preds = blockNode.getPredecessors();
					if (preds.size() == 1) {
						blockNode = preds.get(0);
					} else {
						mth.addWarn("Failed to insert additional move for type inference");
						return false;
					}
				}

				int regNum = reg.getRegNum();
				RegisterArg resultArg = reg.duplicate(regNum, null);
				SSAVar newSsaVar = mth.makeNewSVar(regNum, resultArg);
				RegisterArg arg = reg.duplicate(regNum, var);

				InsnNode moveInsn = new InsnNode(InsnType.MOVE, 1);
				moveInsn.setResult(resultArg);
				moveInsn.addArg(arg);
				moveInsn.add(AFlag.SYNTHETIC);
				blockNode.getInstructions().add(moveInsn);

				phiInsn.replaceArg(reg, reg.duplicate(regNum, newSsaVar));
				return true;
			}
		}
		return false;
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
			for (String ancestor : clsp.getAncestors(objType.getObject())) {
				ArgType ancestorType = ArgType.object(ancestor);
				TypeUpdateResult result = typeUpdate.applyWithWiderAllow(var, ancestorType);
				if (result == TypeUpdateResult.CHANGED) {
					return true;
				}
			}
		}
		return false;
	}

	private void processIncompatiblePrimitives(MethodNode mth, SSAVar var) {
		if (var.getTypeInfo().getType() == ArgType.BOOLEAN) {
			for (ITypeBound bound : var.getTypeInfo().getBounds()) {
				if (bound.getBound() == BoundEnum.USE
						&& bound.getType().isPrimitive() && bound.getType() != ArgType.BOOLEAN) {
					InsnNode insn = bound.getArg().getParentInsn();
					if (insn == null || insn.getType() == InsnType.CAST) {
						continue;
					}

					IndexInsnNode castNode = new IndexInsnNode(InsnType.CAST, bound.getType(), 1);
					castNode.addArg(bound.getArg());
					castNode.setResult(InsnArg.reg(bound.getArg().getRegNum(), bound.getType()));

					SSAVar newVar = mth.makeNewSVar(castNode.getResult().getRegNum(), castNode.getResult());
					CodeVar codeVar = new CodeVar();
					codeVar.setType(bound.getType());
					newVar.setCodeVar(codeVar);
					newVar.getTypeInfo().setType(bound.getType());

					for (int i = insn.getArgsCount() - 1; i >= 0; i--) {
						if (insn.getArg(i) == bound.getArg()) {
							insn.setArg(i, castNode.getResult().duplicate());
							break;
						}
					}

					BlockNode blockNode = BlockUtils.getBlockByInsn(mth, insn);
					List<InsnNode> insnList = blockNode.getInstructions();
					insnList.add(insnList.indexOf(insn), castNode);
				}
			}
		}
	}
}
