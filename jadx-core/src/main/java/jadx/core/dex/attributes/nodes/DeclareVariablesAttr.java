package jadx.core.dex.attributes.nodes;

import java.util.LinkedList;
import java.util.List;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttribute;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.utils.Utils;

/**
 * List of variables to be declared at region start.
 */
public class DeclareVariablesAttr implements IAttribute {

	private final List<RegisterArg> vars = new LinkedList<>();

	public Iterable<RegisterArg> getVars() {
		return vars;
	}

	public void addVar(RegisterArg arg) {
		vars.add(arg);
	}

	@Override
	public AType<DeclareVariablesAttr> getType() {
		return AType.DECLARE_VARIABLES;
	}

	@Override
	public String toString() {
		return "DECL_VAR: " + Utils.listToString(vars);
	}
}
