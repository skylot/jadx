package jadx.core.dex.visitors.typeinference;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
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

	private TypeUpdate typeUpdate;

	@Override
	public void init(RootNode root) {
		typeUpdate = root.getTypeUpdate();
	}

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}
		// collect initial type bounds from assign and usages
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
					&& !var.getAssign().isTypeImmutable()
					&& !tryDeduceType(mth, var, type)) {
				resolved = false;
			}
		}
		if (resolved) {
			for (SSAVar var : new ArrayList<>(mth.getSVars())) {
				processIncompatiblePrimitives(mth, var);
			}
		} else {
			for (SSAVar var : new ArrayList<>(mth.getSVars())) {
				tryInsertAdditionalInsn(mth, var);
			}
			runMultiVariableSearch(mth);
		}
	}

	private void runMultiVariableSearch(MethodNode mth) {
		TypeSearch typeSearch = new TypeSearch(mth);
		try {
			boolean success = typeSearch.run();
			if (!success) {
				mth.addWarn("Multi-variable type inference failed");
			}
		} catch (Exception e) {
			mth.addWarn("Multi-variable type inference failed. Error: " + Utils.getStackTrace(e));
		}
	}

	private boolean setImmutableType(SSAVar ssaVar) {
		try {
			ArgType codeVarType = ssaVar.getCodeVar().getType();
			if (codeVarType != null) {
				return applyImmutableType(ssaVar, codeVarType);
			}
			RegisterArg assignArg = ssaVar.getAssign();
			if (assignArg.isTypeImmutable()) {
				return applyImmutableType(ssaVar, assignArg.getInitType());
			}
			if (ssaVar.contains(AFlag.IMMUTABLE_TYPE)) {
				for (RegisterArg arg : ssaVar.getUseList()) {
					if (arg.isTypeImmutable()) {
						return applyImmutableType(ssaVar, arg.getInitType());
					}
				}
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
				LOG.warn("Initial immutable type set rejected: {} -> {}", ssaVar, initType);
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
				.max(typeUpdate.getArgTypeComparator());
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
		PhiInsn usedInPhi = ssaVar.getUsedInPhi();
		if (usedInPhi != null) {
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
		InsnNode insn = assign.getParentInsn();
		if (insn == null || assign.isTypeImmutable()) {
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

			default:
				ArgType type = insn.getResult().getInitType();
				addBound(typeInfo, new TypeBoundConst(BoundEnum.ASSIGN, type));
				break;
		}
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
		PhiInsn phiInsn = var.getUsedInPhi();
		if (phiInsn == null) {
			return false;
		}
		if (var.getUseCount() == 1) {
			InsnNode assignInsn = var.getAssign().getAssignInsn();
			if (assignInsn != null && assignInsn.getType() == InsnType.MOVE) {
				return false;
			}
		}
		for (Map.Entry<RegisterArg, BlockNode> entry : phiInsn.getBlockBinds().entrySet()) {
			RegisterArg reg = entry.getKey();
			if (reg.getSVar() == var) {
				BlockNode blockNode = entry.getValue();
				InsnNode lastInsn = BlockUtils.getLastInsn(blockNode);
				if (lastInsn != null && BlockSplitter.makeSeparate(lastInsn.getType())) {
					if (Consts.DEBUG) {
						LOG.warn("Can't insert move for PHI in block with separate insn: {}", lastInsn);
					}
					return false;
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

				attachBounds(var);
				for (InsnArg phiArg : phiInsn.getArguments()) {
					attachBounds(((RegisterArg) phiArg).getSVar());
				}
				for (InsnArg phiArg : phiInsn.getArguments()) {
					mergePhiBounds(((RegisterArg) phiArg).getSVar());
				}
				InitCodeVariables.initCodeVar(newSsaVar);
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
		if (var.getAssign().getType() == ArgType.BOOLEAN) {
			for (ITypeBound bound : var.getTypeInfo().getBounds()) {
				if (bound.getBound() == BoundEnum.USE
						&& bound.getType().isPrimitive() && bound.getType() != ArgType.BOOLEAN) {
					InsnNode insn = bound.getArg().getParentInsn();
					if (insn.getType() == InsnType.CAST) {
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
