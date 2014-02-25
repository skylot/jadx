package jadx.core.dex.attributes;

import jadx.core.dex.info.FieldInfo;

public class FieldReplaceAttr implements IAttribute {

	private final FieldInfo fieldInfo;
	private final boolean isOuterClass;

	public FieldReplaceAttr(FieldInfo fieldInfo, boolean isOuterClass) {
		this.fieldInfo = fieldInfo;
		this.isOuterClass = isOuterClass;
	}

	public FieldInfo getFieldInfo() {
		return fieldInfo;
	}

	public boolean isOuterClass() {
		return isOuterClass;
	}

	@Override
	public AttributeType getType() {
		return AttributeType.FIELD_REPLACE;
	}

	@Override
	public String toString() {
		return "REPLACE: " + fieldInfo;
	}
}
