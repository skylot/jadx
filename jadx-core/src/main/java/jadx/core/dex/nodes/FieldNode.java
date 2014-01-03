package jadx.core.dex.nodes;

import jadx.core.dex.attributes.LineAttrNode;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.AccessInfo.AFType;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.instructions.args.ArgType;

import com.android.dx.io.ClassData.Field;

public class FieldNode extends LineAttrNode {

	private final FieldInfo fieldInfo;
	private final AccessInfo accFlags;

	private ArgType type; // store signature

	public FieldNode(ClassNode cls, Field field) {
		this.fieldInfo = FieldInfo.fromDex(cls.dex(), field.getFieldIndex());
		this.type = fieldInfo.getType();
		this.accFlags = new AccessInfo(field.getAccessFlags(), AFType.FIELD);
	}

	public FieldInfo getFieldInfo() {
		return fieldInfo;
	}

	public AccessInfo getAccessFlags() {
		return accFlags;
	}

	public String getName() {
		return fieldInfo.getName();
	}

	public ArgType getType() {
		return type;
	}

	public void setType(ArgType type) {
		this.type = type;
	}

	@Override
	public int hashCode() {
		return fieldInfo.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		FieldNode other = (FieldNode) obj;
		return fieldInfo.equals(other.fieldInfo);
	}

	@Override
	public String toString() {
		return fieldInfo.getDeclClass() + "." + fieldInfo.getName() + " " + type;
	}
}
