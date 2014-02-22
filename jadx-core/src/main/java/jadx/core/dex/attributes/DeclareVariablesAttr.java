package jadx.core.dex.attributes;

import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.utils.Utils;

import java.util.LinkedList;
import java.util.List;

/**
 * List of variables to be declared at region start.
 */
public class DeclareVariablesAttr implements IAttribute {

	private final List<RegisterArg> vars = new LinkedList<RegisterArg>();

	public Iterable<RegisterArg> getVars() {
		return vars;
	}

	public void addVar(RegisterArg arg) {
		vars.add(arg);
	}

	@Override
	public AttributeType getType() {
		return AttributeType.DECLARE_VARIABLES;
	}

	@Override
	public String toString() {
		return "DECL_VAR: " + Utils.listToString(vars);
	}
}
