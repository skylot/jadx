package jadx.core.dex.visitors.regions;

import jadx.core.dex.attributes.AttributeFlag;
import jadx.core.dex.attributes.AttributeType;
import jadx.core.dex.attributes.DeclareVariablesAttr;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.RegionUtils;
import jadx.core.utils.exceptions.JadxException;

import java.util.ArrayList;
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
		private IRegion argRegion;
		private final Set<IRegion> usage = new HashSet<IRegion>(2);
		private final Set<IRegion> assigns = new HashSet<IRegion>(2);

		public void setArg(RegisterArg arg) {
			this.arg = arg;
		}

		public RegisterArg getArg() {
			return arg;
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

	@Override
	public void visit(MethodNode mth) throws JadxException {
		final Map<Variable, Usage> usageMap = new LinkedHashMap<Variable, Usage>();

		// collect all variables usage
		IRegionVisitor collect = new TracedRegionVisitor() {
			@Override
			public void processBlockTraced(MethodNode mth, IBlock container, IRegion curRegion) {
				int len = container.getInstructions().size();
				List<RegisterArg> args = new ArrayList<RegisterArg>();
				for (int i = 0; i < len; i++) {
					InsnNode insn = container.getInstructions().get(i);
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
		};
		DepthRegionTraversal.traverseAll(mth, collect);

		// reduce assigns map
		List<RegisterArg> mthArgs = mth.getArguments(true);
		for (RegisterArg arg : mthArgs) {
			usageMap.remove(new Variable(arg));
		}

		for (Iterator<Entry<Variable, Usage>> it = usageMap.entrySet().iterator(); it.hasNext(); ) {
			Entry<Variable, Usage> entry = it.next();
			Usage u = entry.getValue();

			// if no assigns => remove
			if (u.getAssigns().isEmpty()) {
				it.remove();
				continue;
			}

			// check if we can declare variable at current assigns
			for (IRegion assignRegion : u.getAssigns()) {
				if (u.getArgRegion() == assignRegion
						&& canDeclareInRegion(u, assignRegion)) {
					u.getArg().getParentInsn().getAttributes().add(AttributeFlag.DECLARE_VAR);
					it.remove();
					break;
				}
			}
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
			if (set.isEmpty()) {
				continue;
			}
			IRegion region = set.iterator().next();
			IRegion parent = region;
			boolean declare = false;
			while (parent != null) {
				if (canDeclareInRegion(u, region)) {
					declareVar(region, u.getArg());
					declare = true;
					break;
				}
				region = parent;
				parent = region.getParent();
			}

			if (!declare) {
				declareVar(mth.getRegion(), u.getArg());
			}
		}
	}

	Usage addToUsageMap(RegisterArg arg, Map<Variable, Usage> usageMap) {
		Variable varId = new Variable(arg);
		Usage usage = usageMap.get(varId);
		if (usage == null) {
			usage = new Usage();
			usageMap.put(varId, usage);
		}
		return usage;
	}

	private static void declareVar(IContainer region, RegisterArg arg) {
		DeclareVariablesAttr dv = (DeclareVariablesAttr) region.getAttributes().get(AttributeType.DECLARE_VARIABLES);
		if (dv == null) {
			dv = new DeclareVariablesAttr();
			region.getAttributes().add(dv);
		}
		dv.addVar(arg);
	}

	private static boolean canDeclareInRegion(Usage u, IRegion region) {
		for (IRegion r : u.getAssigns()) {
			if (!RegionUtils.isRegionContainsRegion(region, r)) {
				return false;
			}
		}
		for (IRegion r : u.getUseRegions()) {
			if (!RegionUtils.isRegionContainsRegion(region, r)) {
				return false;
			}
		}
		return true;
	}
}
