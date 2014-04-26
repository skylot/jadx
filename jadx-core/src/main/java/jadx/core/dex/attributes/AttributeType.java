package jadx.core.dex.attributes;

public enum AttributeType {

	/* Multi attributes */

	JUMP(false),

	LOOP(false),
	CATCH_BLOCK(false),

	/* Uniq attributes */

	EXC_HANDLER(true),
	SPLITTER_BLOCK(true),
	FORCE_RETURN(true),

	FIELD_VALUE(true),

	JADX_ERROR(true),
	METHOD_INLINE(true),
	FIELD_REPLACE(true),

	ENUM_CLASS(true),

	ANNOTATION_LIST(true),
	ANNOTATION_MTH_PARAMETERS(true),

	PHI_LIST(true),

	SOURCE_FILE(true),

	// for regions
	DECLARE_VARIABLES(true);

	private static final int NOT_UNIQ_COUNT;
	private final boolean uniq;

	private AttributeType(boolean isUniq) {
		this.uniq = isUniq;
	}

	static {
		// place all not unique attributes at first
		int last = -1;
		AttributeType[] vals = AttributeType.values();
		for (int i = 0; i < vals.length; i++) {
			AttributeType type = vals[i];
			if (type.notUniq()) {
				last = i;
			}
		}
		NOT_UNIQ_COUNT = last + 1;
	}

	public static int getNotUniqCount() {
		return NOT_UNIQ_COUNT;
	}

	public boolean isUniq() {
		return uniq;
	}

	public boolean notUniq() {
		return !uniq;
	}
}
