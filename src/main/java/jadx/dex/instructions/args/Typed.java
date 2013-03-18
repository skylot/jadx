package jadx.dex.instructions.args;

public abstract class Typed {

	protected TypedVar typedVar;

	public TypedVar getTypedVar() {
		return typedVar;
	}

	public void setTypedVar(TypedVar arg) {
		this.typedVar = arg;
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

	public void replace(Typed var) {
		replace(var.getTypedVar());
	}

	public void replace(TypedVar newVar) {
		assert newVar != null;
		if (typedVar == newVar)
			return;

		if (typedVar != null) {
			newVar.merge(typedVar);
			for (InsnArg arg : typedVar.getUseList()) {
				if (arg != this)
					arg.setTypedVar(newVar);
			}
			newVar.getUseList().addAll(typedVar.getUseList());
			if (typedVar.getName() != null)
				newVar.setName(typedVar.getName());
			typedVar.getUseList().clear();
		}
		typedVar = newVar;
	}

}
