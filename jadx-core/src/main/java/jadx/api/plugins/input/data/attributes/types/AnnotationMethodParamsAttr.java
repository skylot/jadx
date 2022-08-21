package jadx.api.plugins.input.data.attributes.types;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.annotations.IAnnotation;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.input.data.attributes.PinnedAttribute;

public class AnnotationMethodParamsAttr extends PinnedAttribute {

	@Nullable
	public static AnnotationMethodParamsAttr pack(List<List<IAnnotation>> annotationRefList) {
		if (annotationRefList.isEmpty()) {
			return null;
		}
		List<AnnotationsAttr> list = new ArrayList<>(annotationRefList.size());
		for (List<IAnnotation> annList : annotationRefList) {
			list.add(AnnotationsAttr.pack(annList));
		}
		return new AnnotationMethodParamsAttr(list);
	}

	private final List<AnnotationsAttr> paramList;

	private AnnotationMethodParamsAttr(List<AnnotationsAttr> paramsList) {
		this.paramList = paramsList;
	}

	public List<AnnotationsAttr> getParamList() {
		return paramList;
	}

	@Override
	public JadxAttrType<AnnotationMethodParamsAttr> getAttrType() {
		return JadxAttrType.ANNOTATION_MTH_PARAMETERS;
	}

	@Override
	public String toString() {
		return paramList.toString();
	}
}
