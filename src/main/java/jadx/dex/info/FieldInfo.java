package jadx.dex.info;

import jadx.dex.attributes.AttrNode;
import jadx.dex.instructions.args.ArgType;
import jadx.dex.nodes.DexNode;

import com.android.dx.io.FieldId;

public class FieldInfo extends AttrNode {

	private final String name;
	private final ArgType type;

	private final ClassInfo declClass;

	public static FieldInfo fromDex(DexNode dex, int index) {
		return new FieldInfo(dex, index);
	}

	protected FieldInfo(DexNode dex, int ind) {
		FieldId field = dex.getFieldId(ind);
		this.name = dex.getString(field.getNameIndex());
		this.type = dex.getType(field.getTypeIndex());
		this.declClass = ClassInfo.fromDex(dex, field.getDeclaringClassIndex());
	}

	public static String getNameById(DexNode dex, int ind) {
		return dex.getString(dex.getFieldId(ind).getNameIndex());
	}

	public String getName() {
		return name;
	}

	public ArgType getType() {
		return type;
	}

	public ClassInfo getDeclClass() {
		return declClass;
	}

	@Override
	public String toString() {
		return declClass + "." + name + " " + type;
	}
}
