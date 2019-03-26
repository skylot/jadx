package jadx.core.dex.attributes.nodes;

import java.util.ArrayList;
import java.util.List;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttribute;
import jadx.core.dex.instructions.args.CodeVar;
import jadx.core.utils.Utils;

/**
 * List of variables to be declared at region start.
 */
public class DeclareVariablesAttr implements IAttribute {

	private final List<CodeVar> vars = new ArrayList<>();

	public Iterable<CodeVar> getVars() {
		return vars;
	}

	public void addVar(CodeVar arg) {
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
