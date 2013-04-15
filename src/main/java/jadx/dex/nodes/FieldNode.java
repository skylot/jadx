package jadx.dex.nodes;

import jadx.dex.attributes.AttrNode;
import jadx.dex.info.AccessInfo;
import jadx.dex.info.AccessInfo.AFType;
import jadx.dex.info.ClassInfo;
import jadx.dex.info.FieldInfo;
import jadx.dex.instructions.args.ArgType;

import com.android.dx.io.ClassData.Field;

public class FieldNode extends AttrNode {

	private final AccessInfo accFlags;
	private final String name;
	private final ClassInfo declClass;

	private ArgType type;

	public FieldNode(ClassNode cls, Field field) {
		FieldInfo f = FieldInfo.fromDex(cls.dex(), field.getFieldIndex());
		this.name = f.getName();
		this.type = f.getType();
		this.declClass = f.getDeclClass();
		this.accFlags = new AccessInfo(field.getAccessFlags(), AFType.FIELD);
	}

	public AccessInfo getAccessFlags() {
		return accFlags;
	}

	public String getName() {
		return name;
	}

	public ArgType getType() {
		return type;
	}

	public void setType(ArgType type) {
		this.type = type;
	}

	public ClassInfo getDeclClass() {
		return declClass;
	}

	@Override
	public String toString() {
		return declClass + "." + name + " " + type;
	}
}
