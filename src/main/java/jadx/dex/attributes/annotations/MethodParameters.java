package jadx.dex.attributes.annotations;

import jadx.dex.attributes.AttributeType;
import jadx.dex.attributes.IAttribute;
import jadx.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class MethodParameters implements IAttribute {

	private final List<AnnotationsList> paramList;

	public MethodParameters(int paramCount) {
		paramList = new ArrayList<AnnotationsList>(paramCount);
	}

	public List<AnnotationsList> getParamList() {
		return paramList;
	}

	@Override
	public AttributeType getType() {
		return AttributeType.ANNOTATION_MTH_PARAMETERS;
	}

	@Override
	public String toString() {
		return Utils.listToString(paramList);
	}

}
