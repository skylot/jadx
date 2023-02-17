package jadx.core.dex.visitors.regions.variables;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.DeclareVariablesAttr;
import jadx.core.dex.instructions.BaseInvokeNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.CodeVar;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.loops.LoopRegion;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.regions.AbstractRegionVisitor;
import jadx.core.dex.visitors.regions.DepthRegionTraversal;
import jadx.core.dex.visitors.typeinference.TypeCompare;
import jadx.core.dex.visitors.typeinference.TypeCompareEnum;
import jadx.core.utils.ListUtils;
import jadx.core.utils.RegionUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxException;

public class ProcessVariables extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(ProcessVariables.class);

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode() || mth.getSVars().isEmpty()) {
			return;
		}
		removeUnusedResults(mth);

		List<CodeVar> codeVars = collectCodeVars(mth);
		if (codeVars.isEmpty()) {
			return;
		}
		checkCodeVars(mth, codeVars);
		// TODO: reduce code vars by name if debug info applied (need checks for variable scopes)

		// collect all variables usage
		CollectUsageRegionVisitor usageCollector = new CollectUsageRegionVisitor();
		DepthRegionTraversal.traverse(mth, usageCollector);
		Map<SSAVar, VarUsage> ssaUsageMap = usageCollector.getUsageMap();
		if (ssaUsageMap.isEmpty()) {
			return;
		}

		Map<CodeVar, List<VarUsage>> codeVarUsage = mergeUsageMaps(codeVars, ssaUsageMap);

		for (Entry<CodeVar, List<VarUsage>> entry : codeVarUsage.entrySet()) {
			declareVar(mth, entry.getKey(), entry.getValue());
		}
	}

	private static void removeUnusedResults(MethodNode mth) {
		DepthRegionTraversal.traverse(mth, new AbstractRegionVisitor() {
			@Override
			public void processBlock(MethodNode mth, IBlock container) {
				for (InsnNode insn : container.getInstructions()) {
					RegisterArg resultArg = insn.getResult();
					if (resultArg == null) {
						continue;
					}
					SSAVar ssaVar = resultArg.getSVar();
					if (isVarUnused(mth, ssaVar)) {
						boolean remove = false;
						if (insn.canRemoveResult()) {
							// remove unused result
							remove = true;
						} else if (insn.isConstInsn()) {
							// remove whole insn
							insn.add(AFlag.REMOVE);
							insn.add(AFlag.DONT_GENERATE);
							remove = true;
						}
						if (remove) {
							insn.setResult(null);
							mth.removeSVar(ssaVar);
							for (RegisterArg arg : ssaVar.getUseList()) {
								arg.resetSSAVar();
							}
						}
					}
				}
			}

			private boolean isVarUnused(MethodNode mth, @Nullable SSAVar ssaVar) {
				if (ssaVar == null) {
					return true;
				}
				List<RegisterArg> useList = ssaVar.getUseList();
				if (useList.isEmpty()) {
					return true;
				}
				if (ssaVar.isUsedInPhi()) {
					return false;
				}
				return ListUtils.allMatch(useList, arg -> isArgUnused(mth, arg));
			}

			private boolean isArgUnused(MethodNode mth, RegisterArg arg) {
				if (arg.contains(AFlag.REMOVE) || arg.contains(AFlag.SKIP_ARG)) {
					return true;
				}
				InsnNode parentInsn = arg.getParentInsn();
				if (parentInsn instanceof BaseInvokeNode
						&& mth.root().getMethodUtils().isSkipArg(((BaseInvokeNode) parentInsn), arg)) {
					arg.add(AFlag.DONT_GENERATE);
					arg.add(AFlag.REMOVE);
					return true;
				}
				return false;
			}
		});
	}

	private void checkCodeVars(MethodNode mth, List<CodeVar> codeVars) {
		int unknownTypesCount = 0;
		for (CodeVar codeVar : codeVars) {
			ArgType codeVarType = codeVar.getType();
			if (codeVarType == null) {
				codeVar.setType(ArgType.UNKNOWN);
				unknownTypesCount++;
			} else {
				codeVar.getSsaVars()
						.forEach(ssaVar -> {
							ArgType ssaType = ssaVar.getImmutableType();
							if (ssaType != null && ssaType.isTypeKnown()) {
								TypeCompare comparator = mth.root().getTypeUpdate().getTypeCompare();
								TypeCompareEnum result = comparator.compareTypes(ssaType, codeVarType);
								if (result == TypeCompareEnum.CONFLICT || result.isNarrow()) {
									mth.addWarn("Incorrect type for immutable var: ssa=" + ssaType
											+ ", code=" + codeVarType
											+ ", for " + ssaVar.getDetailedVarInfo(mth));
								}
							}
						});
			}
		}
		if (unknownTypesCount != 0) {
			mth.addWarn("Unknown variable types count: " + unknownTypesCount);
		}
	}

	private void declareVar(MethodNode mth, CodeVar codeVar, List<VarUsage> usageList) {
		if (codeVar.isDeclared()) {
			return;
		}

		VarUsage mergedUsage = new VarUsage(null);
		for (VarUsage varUsage : usageList) {
			mergedUsage.getAssigns().addAll(varUsage.getAssigns());
			mergedUsage.getUses().addAll(varUsage.getUses());
		}
		if (mergedUsage.getAssigns().isEmpty() && mergedUsage.getUses().isEmpty()) {
			return;
		}

		// check if variable can be declared at one of assigns
		if (checkDeclareAtAssign(usageList, mergedUsage)) {
			return;
		}
		// TODO: search closest region for declare

		// region not found, declare at method start
		declareVarInRegion(mth.getRegion(), codeVar);
	}

	private List<CodeVar> collectCodeVars(MethodNode mth) {
		Map<CodeVar, List<SSAVar>> codeVars = new LinkedHashMap<>();
		for (SSAVar ssaVar : mth.getSVars()) {
			if (ssaVar.getCodeVar().isThis()) {
				continue;
			}
			CodeVar codeVar = ssaVar.getCodeVar();
			List<SSAVar> list = codeVars.computeIfAbsent(codeVar, k -> new ArrayList<>());
			list.add(ssaVar);
		}

		for (Entry<CodeVar, List<SSAVar>> entry : codeVars.entrySet()) {
			CodeVar codeVar = entry.getKey();
			List<SSAVar> list = entry.getValue();
			for (SSAVar ssaVar : list) {
				CodeVar localCodeVar = ssaVar.getCodeVar();
				codeVar.mergeFlagsFrom(localCodeVar);
			}
			if (list.size() > 1) {
				for (SSAVar ssaVar : list) {
					ssaVar.setCodeVar(codeVar);
				}
			}
			codeVar.setSsaVars(list);
		}
		return new ArrayList<>(codeVars.keySet());
	}

	private Map<CodeVar, List<VarUsage>> mergeUsageMaps(List<CodeVar> codeVars, Map<SSAVar, VarUsage> ssaUsageMap) {
		Map<CodeVar, List<VarUsage>> codeVarUsage = new LinkedHashMap<>(codeVars.size());
		for (CodeVar codeVar : codeVars) {
			List<VarUsage> list = new ArrayList<>();
			for (SSAVar ssaVar : codeVar.getSsaVars()) {
				VarUsage usage = ssaUsageMap.get(ssaVar);
				if (usage != null) {
					list.add(usage);
				}
			}
			codeVarUsage.put(codeVar, Utils.lockList(list));
		}
		return codeVarUsage;
	}

	private boolean checkDeclareAtAssign(List<VarUsage> list, VarUsage mergedUsage) {
		if (mergedUsage.getAssigns().isEmpty()) {
			return false;
		}
		for (VarUsage u : list) {
			for (UsePlace assign : u.getAssigns()) {
				if (canDeclareAt(mergedUsage, assign)) {
					return checkDeclareAtAssign(u.getVar());
				}
			}
		}
		return false;
	}

	private static boolean canDeclareAt(VarUsage usage, UsePlace usePlace) {
		IRegion region = usePlace.getRegion();
		// workaround for declare variables used in several loops
		if (region instanceof LoopRegion) {
			for (UsePlace use : usage.getAssigns()) {
				if (!RegionUtils.isRegionContainsRegion(region, use.getRegion())) {
					return false;
				}
			}
		}
		// can't declare in else-if chain between 'else' and next 'if'
		if (region.contains(AFlag.ELSE_IF_CHAIN)) {
			return false;
		}
		return isAllUseAfter(usePlace, usage.getAssigns())
				&& isAllUseAfter(usePlace, usage.getUses());
	}

	/**
	 * Check if all {@code usePlaces} are after {@code checkPlace}
	 */
	private static boolean isAllUseAfter(UsePlace checkPlace, List<UsePlace> usePlaces) {

		IRegion region = checkPlace.getRegion();
		IBlock block = checkPlace.getBlock();
		Set<UsePlace> toCheck = new HashSet<>(usePlaces);
		boolean blockFound = false;
		for (IContainer subBlock : region.getSubBlocks()) {
			if (!blockFound && subBlock == block) {
				blockFound = true;
			}
			if (blockFound) {
				toCheck.removeIf(usePlace -> isContainerContainsUsePlace(subBlock, usePlace));
				if (toCheck.isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean isContainerContainsUsePlace(IContainer subBlock, UsePlace usePlace) {
		if (subBlock == usePlace.getBlock()) {
			return true;
		}
		if (subBlock instanceof IRegion) {
			// TODO: make index for faster check
			return RegionUtils.isRegionContainsRegion(subBlock, usePlace.getRegion());
		}
		return false;
	}

	private static boolean checkDeclareAtAssign(SSAVar var) {
		RegisterArg arg = var.getAssign();
		InsnNode parentInsn = arg.getParentInsn();
		if (parentInsn == null
				|| parentInsn.contains(AFlag.WRAPPED)
				|| parentInsn.getType() == InsnType.PHI) {
			return false;
		}
		if (!arg.equals(parentInsn.getResult())) {
			return false;
		}
		parentInsn.add(AFlag.DECLARE_VAR);
		return true;
	}

	private static void declareVarInRegion(IContainer region, CodeVar var) {
		if (var.isDeclared()) {
			LOG.warn("Try to declare already declared variable: {}", var);
			return;
		}
		DeclareVariablesAttr dv = region.get(AType.DECLARE_VARIABLES);
		if (dv == null) {
			dv = new DeclareVariablesAttr();
			region.addAttr(dv);
		}
		dv.addVar(var);
		var.setDeclared(true);
	}
}
