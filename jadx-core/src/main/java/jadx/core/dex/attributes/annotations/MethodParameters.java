package jadx.core.dex.attributes.annotations;

import java.util.ArrayList;
import java.util.List;

import jadx.api.plugins.input.data.annotations.IAnnotation;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttribute;
import jadx.core.dex.nodes.ICodeNode;
import jadx.core.utils.Utils;

public class MethodParameters implements IAttribute {

	public static void attach(ICodeNode node, List<List<IAnnotation>> annotationRefList) {
		if (annotationRefList.isEmpty()) {
			return;
		}
		List<AnnotationsList> list = new ArrayList<>(annotationRefList.size());
		for (List<IAnnotation> annList : annotationRefList) {
			list.add(AnnotationsList.pack(annList));
		}
		node.addAttr(new MethodParameters(list));
	}

	private final List<AnnotationsList> paramList;

	public MethodParameters(List<AnnotationsList> paramsList) {
		this.paramList = paramsList;
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
