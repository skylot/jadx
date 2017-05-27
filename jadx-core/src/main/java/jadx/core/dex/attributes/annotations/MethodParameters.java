package jadx.core.dex.attributes.annotations;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttribute;
import jadx.core.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class MethodParameters implements IAttribute {

	private final List<AnnotationsList> paramList;

	public MethodParameters(int paramCount) {
		paramList = new ArrayList<>(paramCount);
	}

	public List<AnnotationsList> getParamList() {
		return paramList;
	}

	@Override
	public AType<MethodParameters> getType() {
		return AType.ANNOTATION_MTH_PARAMETERS;
	}

	@Override
	public String toString() {
		return Utils.listToString(paramList);
	}

}
