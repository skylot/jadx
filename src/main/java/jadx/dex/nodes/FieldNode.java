package jadx.dex.nodes;

import jadx.dex.info.AccessInfo;
import jadx.dex.info.AccessInfo.AFType;
import jadx.dex.info.FieldInfo;

import com.android.dx.io.ClassData.Field;

public class FieldNode extends FieldInfo {

	private final AccessInfo accFlags;

	public FieldNode(ClassNode cls, Field field) {
		super(cls.dex(), field.getFieldIndex());
		this.accFlags = new AccessInfo(field.getAccessFlags(), AFType.FIELD);
	}

	public AccessInfo getAccessFlags() {
		return accFlags;
	}

}
