package jadx.dex.attributes;

public enum AttributeType {
	// TODO? add attribute target (insn, block, method, field, class)

	// instructions
	JUMP(false),

	// blocks
	LOOP(false),

	CATCH_BLOCK(false),
	EXC_HANDLER(true),

	SPLITTER_BLOCK(true),

	FORCE_RETURN(true),

	// fields
	FIELD_VALUE(true),

	// methods
	JADX_ERROR(true),

	// classes
	ENUM_CLASS(true),

	// any
	ANNOTATION_LIST(true),
	ANNOTATION_MTH_PARAMETERS(true),

	DECLARE_VARIABLE(true);

	private final boolean uniq;

	private AttributeType(boolean isUniq) {
		this.uniq = isUniq;
	}

	public boolean isUniq() {
		return uniq;
	}

	public boolean notUniq() {
		return !uniq;
	}
}
