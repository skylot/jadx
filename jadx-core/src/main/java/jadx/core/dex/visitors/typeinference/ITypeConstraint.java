package jadx.core.dex.visitors.typeinference;

import java.util.List;

import jadx.core.dex.instructions.args.SSAVar;

public interface ITypeConstraint {

	List<SSAVar> getRelatedVars();

	boolean check(TypeSearchState state);
}
