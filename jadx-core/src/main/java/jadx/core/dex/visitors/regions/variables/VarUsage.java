package jadx.core.dex.visitors.regions.variables;

import java.util.ArrayList;
import java.util.List;

import jadx.core.dex.instructions.args.SSAVar;

class VarUsage {
	private final SSAVar var;
	private final List<UsePlace> assigns = new ArrayList<>(3);
	private final List<UsePlace> uses = new ArrayList<>(3);

	VarUsage(SSAVar var) {
		this.var = var;
	}

	public SSAVar getVar() {
		return var;
	}

	public List<UsePlace> getAssigns() {
		return assigns;
	}

	public List<UsePlace> getUses() {
		return uses;
	}

	@Override
	public String toString() {
		return '{' + (var == null ? "-" : var.toShortString()) + ", a:" + assigns + ", u:" + uses + '}';
	}
}
