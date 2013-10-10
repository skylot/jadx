package jadx.core.dex.instructions.args;

import java.util.List;

public abstract class Typed {

	TypedVar typedVar;

	public TypedVar getTypedVar() {
		return typedVar;
	}

	public ArgType getType() {
		return typedVar.getType();
	}

	public boolean merge(Typed var) {
		return typedVar.merge(var.getTypedVar());
	}

	public boolean merge(ArgType var) {
		return typedVar.merge(var);
	}

	public void forceSetTypedVar(TypedVar arg) {
		this.typedVar = arg;
	}

	public void mergeDebugInfo(Typed arg) {
		merge(arg);
		mergeName(arg);
	}

	protected void mergeName(Typed arg) {
		getTypedVar().mergeName(arg.getTypedVar());
	}

	public boolean replaceTypedVar(Typed var) {
		TypedVar curVar = this.typedVar;
		TypedVar newVar = var.typedVar;
		if (curVar == newVar) {
			return false;
		}
		if (curVar != null) {
			if (curVar.isImmutable()) {
				moveInternals(newVar, curVar);
			} else {
				newVar.merge(curVar);
				moveInternals(curVar, newVar);
				this.typedVar = newVar;
			}
		} else {
			this.typedVar = newVar;
		}
		return true;
	}

	private void moveInternals(TypedVar from, TypedVar to) {
		List<InsnArg> curUseList = from.getUseList();
		if (curUseList.size() != 0) {
			for (InsnArg arg : curUseList) {
				if (arg != this) {
					arg.forceSetTypedVar(to);
				}
			}
			to.getUseList().addAll(curUseList);
			curUseList.clear();
		}
		to.mergeName(from);
	}
}
