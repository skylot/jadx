package jadx.core.dex.instructions.args;

import jadx.core.dex.info.FieldInfo;

// TODO: don't extend RegisterArg (now used as a result of instruction)
public final class FieldArg extends RegisterArg {

	private final FieldInfo field;
	// regArg equal 'null' for static fields
	private final RegisterArg regArg;

	public FieldArg(FieldInfo field, RegisterArg reg) {
		super(-1);
		this.regArg = reg;
		this.field = field;
	}

	public FieldInfo getField() {
		return field;
	}

	public RegisterArg getRegisterArg() {
		return regArg;
	}

	public boolean isStatic() {
		return regArg == null;
	}

	@Override
	public boolean isField() {
		return true;
	}

	@Override
	public boolean isRegister() {
		return false;
	}

	@Override
	public void setType(ArgType type) {
		this.type = type;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof FieldArg) || !super.equals(obj)) {
			return false;
		}
		FieldArg fieldArg = (FieldArg) obj;
		if (!field.equals(fieldArg.field)) {
			return false;
		}
		if (regArg != null ? !regArg.equals(fieldArg.regArg) : fieldArg.regArg != null) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + field.hashCode();
		result = 31 * result + (regArg != null ? regArg.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "(" + field + ")";
	}
}
