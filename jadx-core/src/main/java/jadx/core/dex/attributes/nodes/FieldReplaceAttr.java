package jadx.core.dex.attributes.nodes;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttribute;
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
	public AType<FieldReplaceAttr> getType() {
		return AType.FIELD_REPLACE;
	}

	@Override
	public String toString() {
		return "REPLACE: " + fieldInfo;
	}
}
