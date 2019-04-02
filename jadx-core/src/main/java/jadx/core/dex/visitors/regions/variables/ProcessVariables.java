package jadx.core.dex.visitors.regions.variables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.DeclareVariablesAttr;
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
import jadx.core.dex.visitors.regions.DepthRegionTraversal;
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

		List<CodeVar> codeVars = collectCodeVars(mth);
		if (codeVars.isEmpty()) {
			return;
		}
		checkCodeVars(mth, codeVars);
		// TODO: reduce code vars by name if debug info applied. Need checks for variable scopes before reduce

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

		List<SSAVar> svars = mth.getSVars();
		for (int i1 = 0; i1 < svars.size(); i1++) {
			for (int i2 = 0; i2 < svars.size(); i2++) {
				if (i1 != i2) {
					SSAVar var1 = svars.get(i1);
					SSAVar var2 = svars.get(i2);
					replaceIfPossible(codeVarUsage, var1, var2);
				}
			}
		}
	}

	private static void replaceIfPossible(Map<CodeVar, List<VarUsage>> codeVarUsage,
			SSAVar var1, SSAVar var2) {
		if (var1.getTypeInfo().getType().equals(
				var2.getTypeInfo().getType())) {
			List<VarUsage> usage1 = codeVarUsage.get(var1.getCodeVar());
			List<VarUsage> usage2 = codeVarUsage.get(var2.getCodeVar());
			if (usage1 != null && usage1.size() == 1
					&& usage2 != null && usage2.size() == 1) {
				VarUsage varUsage1 = usage1.get(0);
				VarUsage varUsage2 = usage2.get(0);
				if (canReplace(var1, var2, varUsage1, varUsage2)) {
					RegisterArg arg1 = var1.getAssign();
					RegisterArg arg2 = var2.getAssign();
					InsnNode node = varUsage2.getAssignNodes().get(0);
					node.setResult(arg1);
					node.remove(AFlag.DECLARE_VAR);
					for (InsnNode n : varUsage2.getUseNodes()) {
						n.replaceArg(arg2, arg1);
					}
				}
			}
		}
	}

	private static boolean canReplace(SSAVar var1, SSAVar var2,
			VarUsage varUsage1, VarUsage varUsage2) {
		if (varUsage2.getAssignNodes().size() != 1
				|| varUsage1.getUseNodes().size() != 1
				|| !varUsage1.getAssignNodes().isEmpty()) {
			return false;
		}
		RegisterArg result1 = varUsage1.getUseNodes().get(0).getResult();
		return result1 != null && result1.getSVar().equals(var2);

	}
	private void checkCodeVars(MethodNode mth, List<CodeVar> codeVars) {
		int unknownTypesCount = 0;
		for (CodeVar codeVar : codeVars) {
			codeVar.getSsaVars().stream()
					.filter(ssaVar -> ssaVar.contains(AFlag.IMMUTABLE_TYPE))
					.forEach(ssaVar -> {
						ArgType ssaType = ssaVar.getAssign().getInitType();
						if (ssaType.isTypeKnown() && !ssaType.equals(codeVar.getType())) {
							mth.addWarn("Incorrect type for immutable var: ssa=" + ssaType
									+ ", code=" + codeVar.getType()
									+ ", for " + ssaVar.getDetailedVarInfo(mth));
						}
					});
			if (codeVar.getType() == null) {
				codeVar.setType(ArgType.UNKNOWN);
				unknownTypesCount++;
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
		// search closest region for declare
		if (searchDeclareRegion(mergedUsage, codeVar)) {
			return;
		}
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
		if (parentInsn == null) {
			return false;
		}
		if (!arg.equals(parentInsn.getResult())) {
			return false;
		}
		parentInsn.add(AFlag.DECLARE_VAR);
		return true;
	}

	private boolean searchDeclareRegion(VarUsage u, CodeVar codeVar) {
		/*
		Set<IRegion> set = u.getUseRegions();
		for (Iterator<IRegion> it = set.iterator(); it.hasNext(); ) {
			IRegion r = it.next();
			IRegion parent = r.getParent();
			if (parent != null && set.contains(parent)) {
				it.remove();
			}
		}
		IRegion region = null;
		if (!set.isEmpty()) {
			region = set.iterator().next();
		} else if (!u.getAssigns().isEmpty()) {
			region = u.getAssigns().iterator().next();
		}
		if (region == null) {
			return false;
		}
		IRegion parent = region;
		while (parent != null) {
			if (canDeclareAt(u, region)) {
				declareVarInRegion(region, codeVar);
				return true;
			}
			region = parent;
			parent = region.getParent();
		}
		*/
		return false;
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

	private static boolean isAllRegionsAfter(IRegion region, Set<IRegion> others) {
		IRegion parent = region.getParent();
		if (parent == null) {
			return true;
		}
		// lazy init for
		int regionIndex = -2;
		List<IContainer> subBlocks = Collections.emptyList();
		for (IRegion r : others) {
			if (parent == r.getParent()) {
				// on same level, check order by index
				if (regionIndex == -2) {
					subBlocks = parent.getSubBlocks();
					regionIndex = subBlocks.indexOf(region);
				}
				int rIndex = subBlocks.indexOf(r);
				if (regionIndex > rIndex) {
					return false;
				}
			} else if (!RegionUtils.isRegionContainsRegion(region, r)) {
				return false;
			}
		}
		return true;
	}
}
