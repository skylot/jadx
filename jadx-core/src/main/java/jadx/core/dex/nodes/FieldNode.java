package jadx.core.dex.nodes;

import com.android.dex.ClassData.Field;

import jadx.core.dex.attributes.nodes.LineAttrNode;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.AccessInfo.AFType;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.instructions.args.ArgType;

public class FieldNode extends LineAttrNode {

	private final ClassNode parent;
	private final FieldInfo fieldInfo;
	private final AccessInfo accFlags;

	private ArgType type; // store signature

	public FieldNode(ClassNode cls, Field field) {
		this(cls, FieldInfo.fromDex(cls.dex(), field.getFieldIndex()),
				field.getAccessFlags());
	}

	public FieldNode(ClassNode cls, FieldInfo fieldInfo, int accessFlags) {
		this.parent = cls;
		this.fieldInfo = fieldInfo;
		this.type = fieldInfo.getType();
		this.accFlags = new AccessInfo(accessFlags, AFType.FIELD);
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

	public String getAlias() {
		return fieldInfo.getAlias();
	}

	public ArgType getType() {
		return type;
	}

	public void setType(ArgType type) {
		this.type = type;
	}

	public ClassNode getParentClass() {
		return parent;
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
		return fieldInfo.getDeclClass() + "." + fieldInfo.getName() + " :" + type;
	}
}
