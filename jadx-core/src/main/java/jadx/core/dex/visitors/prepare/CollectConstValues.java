package jadx.core.dex.visitors.prepare;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.ConstStorage;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.JadxVisitor;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "CollectConstValues",
		desc = "Collect and store values from static final fields"
)
public class CollectConstValues extends AbstractVisitor {

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		RootNode root = cls.root();
		if (!root.getArgs().isReplaceConsts()) {
			return true;
		}
		if (cls.getFields().isEmpty()) {
			return true;
		}
		ConstStorage constStorage = root.getConstValues();
		for (FieldNode fld : cls.getFields()) {
			try {
				Object value = getFieldConstValue(fld);
				if (value != null) {
					constStorage.addConstField(fld, value, fld.getAccessFlags().isPublic());
				}
			} catch (Exception e) {
				cls.addWarnComment("Failed to process value of field: " + fld, e);
			}
		}
		return true;
	}

	public static @Nullable Object getFieldConstValue(FieldNode fld) {
		AccessInfo accFlags = fld.getAccessFlags();
		if (accFlags.isStatic() && accFlags.isFinal()) {
			EncodedValue constVal = fld.get(JadxAttrType.CONSTANT_VALUE);
			if (constVal != null && constVal != EncodedValue.NULL) {
				return constVal.getValue();
			}
		}
		return null;
	}
}
