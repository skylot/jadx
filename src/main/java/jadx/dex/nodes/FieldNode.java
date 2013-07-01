package jadx.dex.nodes;

import jadx.dex.attributes.AttrNode;
import jadx.dex.info.AccessInfo;
import jadx.dex.info.AccessInfo.AFType;
import jadx.dex.info.FieldInfo;
import jadx.dex.instructions.args.ArgType;

import com.android.dx.io.ClassData.Field;

public class FieldNode extends AttrNode {

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
	public String toString() {
		return fieldInfo.getDeclClass() + "." + fieldInfo.getName() + " " + type;
	}
}
