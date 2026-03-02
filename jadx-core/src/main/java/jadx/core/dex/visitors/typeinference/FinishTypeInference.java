package jadx.core.dex.visitors.typeinference;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.JadxVisitor;

@JadxVisitor(
		name = "Finish Type Inference",
		desc = "Check used types",
		runAfter = {
				TypeInferenceVisitor.class
		}
)
public final class FinishTypeInference extends AbstractVisitor {

	private TypeUpdate typeUpdate;

	@Override
	public void init(RootNode root) {
		this.typeUpdate = root.getTypeUpdate();
	}

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode() || mth.getSVars().isEmpty()) {
			return;
		}
		mth.getSVars().forEach(var -> {
			ArgType type = var.getTypeInfo().getType();
			if (!type.isTypeKnown()) {
				// works for me at least
				// try last resort to resolve type
				tryLastResort(mth, var);
				type = var.getTypeInfo().getType();
				if (!type.isTypeKnown()) {
					mth.addWarnComment("Type inference failed for: " + var.getDetailedVarInfo(mth));
				}
			}
			ArgType codeVarType = var.getCodeVar().getType();
			if (codeVarType == null) {
				var.getCodeVar().setType(ArgType.UNKNOWN);
			}
		});
	}

	private void tryLastResort(MethodNode mth, SSAVar var) {
		ArgType immutableType = var.getImmutableType();
		if (immutableType != null && immutableType.isTypeKnown()) {
			if (applyForced(mth, var, immutableType)) {
				return;
			}

			var.setType(immutableType);
			return;
		}

		RegisterArg assign = var.getAssign();
		ArgType assignInitType = assign.getInitType();
		if (assignInitType != null && assignInitType.isTypeKnown()) {
			if (applyForced(mth, var, assignInitType)) {
				return;
			}
		}

		ArgType bestBound = selectBestKnownBound(var.getTypeInfo().getBounds());
		if (bestBound != null) {
			TypeUpdateResult result = typeUpdate.applyWithWiderAllow(mth, var, bestBound);
			if (result == TypeUpdateResult.CHANGED) {
				return;
			}

			if (applyForced(mth, var, bestBound)) {
				return;
			}
		}

		ArgType fallback = deduceFallbackType(var);
		if (fallback != null) {
			var.setType(fallback);
		}
	}

	private boolean applyForced(MethodNode mth, SSAVar var, ArgType type) {
		TypeUpdateResult res = typeUpdate.applyWithWiderIgnSame(mth, var, type);
		return res == TypeUpdateResult.CHANGED;
	}

	@Nullable
	private ArgType selectBestKnownBound(Set<ITypeBound> bounds) {
		ArgType best = null;
		TypeCompare cmp = typeUpdate.getTypeCompare();

		for (ITypeBound bound : bounds) {
			if(bound.getBound() != BoundEnum.ASSIGN) {
				continue;
			}

			ArgType t = bound.getType();
			if (t == null || !t.isTypeKnown() || t.isWildcard()) {
				continue;
			}

			if (best == null || cmp.compareTypes(t, best).isNarrow()) {
				best = t;
			}
		}

		if (best != null) {
			return best;
		}

		for (ITypeBound bound : bounds) {
			ArgType t = bound.getType();
			if (t == null || !t.isTypeKnown() || t.isWildcard()) {
				continue;
			}

			if (best == null || cmp.compareTypes(t, best).isNarrow()) {
				best = t;
			}
		}
		return best;
	}

	@Nullable
	private ArgType deduceFallbackType(SSAVar var) {
		Set<ITypeBound> bounds = var.getTypeInfo().getBounds();
		boolean couldBeObject = false;
		boolean couldBeArray = false;
		boolean couldBeBoolean = false;
		boolean couldBeNumber = false;

		for (ITypeBound bound : bounds) {
			ArgType t = bound.getType();
			if (t == null) {
				continue;
			}
			if (t.isObject()) {
				couldBeObject = true;
			} else if (t.isArray()) {
				couldBeArray = true;
			} else if (t.equals(ArgType.BOOLEAN)) {
				couldBeBoolean = true;
			} else if (t.isPrimitive()) {
				couldBeNumber = true;
			} else if (t.canBeObject()) {
				couldBeObject = true;
			}
		}

		if (couldBeObject) {
			return ArgType.OBJECT;
		}
		if (couldBeArray) {
			return ArgType.array(ArgType.OBJECT);
		}
		if (couldBeBoolean) {
			return ArgType.BOOLEAN;
		}
		if (couldBeNumber) {
			return ArgType.INT;
		}

		return null;
	}

	@Override
	public String getName() {
		return "FinishTypeInference";
	}
}
