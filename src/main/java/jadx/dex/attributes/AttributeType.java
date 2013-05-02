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
	METHOD_INLINE(true),

	// classes
	ENUM_CLASS(true),

	// any
	ANNOTATION_LIST(true),
	ANNOTATION_MTH_PARAMETERS(true),

	DECLARE_VARIABLE(true);

	private static final int notUniqCount;
	private final boolean uniq;

	static {
		// place all not unique attributes at first
		int last = -1;
		AttributeType[] vals = AttributeType.values();
		for (int i = 0; i < vals.length; i++) {
			AttributeType type = vals[i];
			if (type.notUniq())
				last = i;
		}
		notUniqCount = last + 1;
	}

	public static int getNotUniqCount() {
		return notUniqCount;
	}

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
