package jadx.core.dex.attributes;

import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.utils.Utils;

import java.util.List;

public class DeclareVariableAttr implements IAttribute {

	private final List<RegisterArg> vars;

	public DeclareVariableAttr() {
		this.vars = null; // for instruction use result
	}

	public DeclareVariableAttr(List<RegisterArg> vars) {
		this.vars = vars; // for regions
	}

	public List<RegisterArg> getVars() {
		return vars;
	}

	public void addVar(RegisterArg arg) {
		int i;
		if ((i = vars.indexOf(arg)) != -1) {
			if (vars.get(i).getType().equals(arg.getType()))
				return;
		}
		vars.add(arg);
	}

	@Override
	public AttributeType getType() {
		return AttributeType.DECLARE_VARIABLE;
	}

	@Override
	public String toString() {
		return "DECL_VAR: " + Utils.listToString(vars);
	}
}
