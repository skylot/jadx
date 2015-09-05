package jadx.core.dex.visitors.regions;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.DeclareVariablesAttr;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.VarName;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IBranchRegion;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.loops.ForLoop;
import jadx.core.dex.regions.loops.LoopRegion;
import jadx.core.dex.regions.loops.LoopType;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.RegionUtils;
import jadx.core.utils.exceptions.JadxException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessVariables extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(ProcessVariables.class);

	private static class Variable {
		private final int regNum;
		private final ArgType type;

		public Variable(RegisterArg arg) {
			this.regNum = arg.getRegNum();
			this.type = arg.getType();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Variable variable = (Variable) o;
			return regNum == variable.regNum && type.equals(variable.type);
		}

		@Override
		public int hashCode() {
			return 31 * regNum + type.hashCode();
		}

		@Override
		public String toString() {
			return regNum + " " + type;
		}
	}

	private static class Usage {
		private RegisterArg arg;
		private VarName varName;
		private IRegion argRegion;
		private final Set<IRegion> usage = new HashSet<IRegion>(2);
		private final Set<IRegion> assigns = new HashSet<IRegion>(2);

		public void setArg(RegisterArg arg) {
			this.arg = arg;
		}

		public RegisterArg getArg() {
			return arg;
		}

		public VarName getVarName() {
			return varName;
		}

		public void setVarName(VarName varName) {
			this.varName = varName;
		}

		public void setArgRegion(IRegion argRegion) {
			this.argRegion = argRegion;
		}

		public IRegion getArgRegion() {
			return argRegion;
		}

		public Set<IRegion> getAssigns() {
			return assigns;
		}

		public Set<IRegion> getUseRegions() {
			return usage;
		}

		@Override
		public String toString() {
			return arg + ", a:" + assigns + ", u:" + usage;
		}
	}

	private static class CollectUsageRegionVisitor extends TracedRegionVisitor {
		private final List<RegisterArg> args;
		private final Map<Variable, Usage> usageMap;

		public CollectUsageRegionVisitor(Map<Variable, Usage> usageMap) {
			this.usageMap = usageMap;
			args = new ArrayList<RegisterArg>();
		}

		@Override
		public void processBlockTraced(MethodNode mth, IBlock container, IRegion curRegion) {
			regionProcess(mth, curRegion);
			int len = container.getInstructions().size();
			for (int i = 0; i < len; i++) {
				InsnNode insn = container.getInstructions().get(i);
				if (insn.contains(AFlag.SKIP)) {
					continue;
				}
				args.clear();
				processInsn(insn, curRegion);
			}
		}

		private void regionProcess(MethodNode mth, IRegion region) {
			if (region instanceof LoopRegion) {
				LoopRegion loopRegion = (LoopRegion) region;
				LoopType loopType = loopRegion.getType();
				if (loopType instanceof ForLoop) {
					ForLoop forLoop = (ForLoop) loopType;
					processInsn(forLoop.getInitInsn(), region);
					processInsn(forLoop.getIncrInsn(), region);
				}
			}
		}

		void processInsn(InsnNode insn, IRegion curRegion) {
			if (insn == null) {
				return;
			}
			// result
			RegisterArg result = insn.getResult();
			if (result != null && result.isRegister()) {
				Usage u = addToUsageMap(result, usageMap);
				if (u.getArg() == null) {
					u.setArg(result);
					u.setArgRegion(curRegion);
				}
				u.getAssigns().add(curRegion);
			}
			// args
			args.clear();
			insn.getRegisterArgs(args);
			for (RegisterArg arg : args) {
				Usage u = addToUsageMap(arg, usageMap);
				u.getUseRegions().add(curRegion);
			}
		}
	}

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()) {
			return;
		}
		final Map<Variable, Usage> usageMap = new LinkedHashMap<Variable, Usage>();
		for (RegisterArg arg : mth.getArguments(true)) {
			addToUsageMap(arg, usageMap);
		}

		// collect all variables usage
		IRegionVisitor collect = new CollectUsageRegionVisitor(usageMap);
		DepthRegionTraversal.traverse(mth, collect);

		// reduce assigns map
		List<RegisterArg> mthArgs = mth.getArguments(true);
		for (RegisterArg arg : mthArgs) {
			usageMap.remove(new Variable(arg));
		}

		Iterator<Entry<Variable, Usage>> umIt = usageMap.entrySet().iterator();
		while (umIt.hasNext()) {
			Entry<Variable, Usage> entry = umIt.next();
			Usage u = entry.getValue();
			// if no assigns => remove
			if (u.getAssigns().isEmpty()) {
				umIt.remove();
				continue;
			}

			// variable declared at 'catch' clause
			InsnNode parentInsn = u.getArg().getParentInsn();
			if (parentInsn == null || parentInsn.getType() == InsnType.MOVE_EXCEPTION) {
				umIt.remove();
			}
		}
		if (usageMap.isEmpty()) {
			return;
		}

		Map<IContainer, Integer> regionsOrder = new HashMap<IContainer, Integer>();
		calculateOrder(mth.getRegion(), regionsOrder, 0, true);

		for (Iterator<Entry<Variable, Usage>> it = usageMap.entrySet().iterator(); it.hasNext(); ) {
			Entry<Variable, Usage> entry = it.next();
			Usage u = entry.getValue();
			// check if variable can be declared at current assigns
			for (IRegion assignRegion : u.getAssigns()) {
				if (u.getArgRegion() == assignRegion
						&& canDeclareInRegion(u, assignRegion, regionsOrder)) {
					if (declareAtAssign(u)) {
						it.remove();
						break;
					}
				}
			}
		}
		if (usageMap.isEmpty()) {
			return;
		}

		// apply
		for (Entry<Variable, Usage> entry : usageMap.entrySet()) {
			Usage u = entry.getValue();

			// find region which contain all usage regions
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
				continue;
			}
			IRegion parent = region;
			boolean declared = false;
			while (parent != null) {
				if (canDeclareInRegion(u, region, regionsOrder)) {
					declareVar(region, u.getArg());
					declared = true;
					break;
				}
				region = parent;
				parent = region.getParent();
			}
			if (!declared) {
				declareVar(mth.getRegion(), u.getArg());
			}
		}
	}

	private static Usage addToUsageMap(RegisterArg arg, Map<Variable, Usage> usageMap) {
		Variable varId = new Variable(arg);
		Usage usage = usageMap.get(varId);
		if (usage == null) {
			usage = new Usage();
			usageMap.put(varId, usage);
		}
		// merge variables names
		if (usage.getVarName() == null) {
			VarName argVN = arg.getSVar().getVarName();
			if (argVN == null) {
				argVN = new VarName();
				arg.getSVar().setVarName(argVN);
			}
			usage.setVarName(argVN);
		} else {
			arg.getSVar().setVarName(usage.getVarName());
		}
		return usage;
	}

	private static boolean declareAtAssign(Usage u) {
		RegisterArg arg = u.getArg();
		InsnNode parentInsn = arg.getParentInsn();
		if (!arg.equals(parentInsn.getResult())) {
			return false;
		}
		parentInsn.add(AFlag.DECLARE_VAR);
		return true;
	}

	private static void declareVar(IContainer region, RegisterArg arg) {
		DeclareVariablesAttr dv = region.get(AType.DECLARE_VARIABLES);
		if (dv == null) {
			dv = new DeclareVariablesAttr();
			region.addAttr(dv);
		}
		dv.addVar(arg);
	}

	private static int calculateOrder(IContainer container, Map<IContainer, Integer> regionsOrder,
			int id, boolean inc) {
		if (!(container instanceof IRegion)) {
			return id;
		}
		IRegion region = (IRegion) container;
		Integer previous = regionsOrder.put(region, id);
		if (previous != null) {
			return id;
		}
		for (IContainer c : region.getSubBlocks()) {
			if (c instanceof IBranchRegion) {
				// on branch set for all inner regions same order id
				id = calculateOrder(c, regionsOrder, inc ? id + 1 : id, false);
			} else {
				List<IContainer> handlers = RegionUtils.getExcHandlersForRegion(c);
				if (!handlers.isEmpty()) {
					for (IContainer handler : handlers) {
						id = calculateOrder(handler, regionsOrder, inc ? id + 1 : id, inc);
					}
				}
				id = calculateOrder(c, regionsOrder, inc ? id + 1 : id, inc);
			}
		}
		return id;
	}

	private static boolean canDeclareInRegion(Usage u, IRegion region, Map<IContainer, Integer> regionsOrder) {
		Integer pos = regionsOrder.get(region);
		if (pos == null) {
			LOG.debug("TODO: Not found order for region {} for {}", region, u);
			return false;
		}
		// workaround for declare variables used in several loops
		if (region instanceof LoopRegion) {
			for (IRegion r : u.getAssigns()) {
				if (!RegionUtils.isRegionContainsRegion(region, r)) {
					return false;
				}
			}
		}
		return isAllRegionsAfter(region, pos, u.getAssigns(), regionsOrder)
				&& isAllRegionsAfter(region, pos, u.getUseRegions(), regionsOrder);
	}

	private static boolean isAllRegionsAfter(IRegion region, int pos,
			Set<IRegion> regions, Map<IContainer, Integer> regionsOrder) {
		for (IRegion r : regions) {
			if (r == region) {
				continue;
			}
			Integer rPos = regionsOrder.get(r);
			if (rPos == null) {
				LOG.debug("TODO: Not found order for region {} in {}", r, regionsOrder);
				return false;
			}
			if (pos > rPos) {
				return false;
			}
			if (pos == rPos) {
				return isAllRegionsAfterRecursive(region, regions);
			}
		}
		return true;
	}

	private static boolean isAllRegionsAfterRecursive(IRegion region, Set<IRegion> others) {
		for (IRegion r : others) {
			if (!RegionUtils.isRegionContainsRegion(region, r)) {
				return false;
			}
		}
		return true;
	}
}
