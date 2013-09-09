package jadx.core.dex.instructions.args;

import jadx.core.dex.info.FieldInfo;

public final class FieldArg extends RegisterArg {

	private final FieldInfo field;

	public FieldArg(FieldInfo field, int regNum) {
		super(regNum, field.getType());
		this.field = field;
	}

	public FieldInfo getField() {
		return field;
	}

	public boolean isStatic() {
		return regNum == -1;
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
	public String toString() {
		return "(" + field + ")";
	}
}
