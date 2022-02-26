package jadx.plugins.input.java.data.attributes.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jadx.api.plugins.input.data.annotations.AnnotationVisibility;
import jadx.api.plugins.input.data.annotations.IAnnotation;
import jadx.api.plugins.input.data.attributes.types.AnnotationMethodParamsAttr;
import jadx.api.plugins.utils.Utils;
import jadx.plugins.input.java.data.attributes.IJavaAttribute;
import jadx.plugins.input.java.data.attributes.IJavaAttributeReader;
import jadx.plugins.input.java.data.attributes.JavaAttrStorage;
import jadx.plugins.input.java.data.attributes.JavaAttrType;

public class JavaParamAnnsAttr implements IJavaAttribute {
	private final List<List<IAnnotation>> list;

	public JavaParamAnnsAttr(List<List<IAnnotation>> list) {
		this.list = list;
	}

	public List<List<IAnnotation>> getList() {
		return list;
	}

	public static IJavaAttributeReader reader(AnnotationVisibility visibility) {
		return (clsData, reader) -> {
			int len = reader.readU1();
			List<List<IAnnotation>> list = new ArrayList<>(len);
			for (int i = 0; i < len; i++) {
				list.add(JavaAnnotationsAttr.readAnnotationsList(visibility, clsData, reader));
			}
			return new JavaParamAnnsAttr(list);
		};
	}

	public static AnnotationMethodParamsAttr merge(JavaAttrStorage storage) {
		JavaParamAnnsAttr runtimeAnnAttr = storage.get(JavaAttrType.RUNTIME_PARAMETER_ANNOTATIONS);
		JavaParamAnnsAttr buildAnnAttr = storage.get(JavaAttrType.BUILD_PARAMETER_ANNOTATIONS);
		if (runtimeAnnAttr == null && buildAnnAttr == null) {
			return null;
		}
		if (buildAnnAttr == null) {
			return AnnotationMethodParamsAttr.pack(runtimeAnnAttr.getList());
		}
		if (runtimeAnnAttr == null) {
			return AnnotationMethodParamsAttr.pack(buildAnnAttr.getList());
		}
		return AnnotationMethodParamsAttr.pack(mergeParamLists(runtimeAnnAttr.getList(), buildAnnAttr.getList()));
	}

	private static List<List<IAnnotation>> mergeParamLists(List<List<IAnnotation>> first, List<List<IAnnotation>> second) {
		int firstSize = first.size();
		int secondSize = second.size();
		int size = Math.max(firstSize, secondSize);
		List<List<IAnnotation>> result = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			List<IAnnotation> firstList = i < firstSize ? first.get(i) : Collections.emptyList();
			List<IAnnotation> secondList = i < secondSize ? second.get(i) : Collections.emptyList();
			result.add(Utils.concat(firstList, secondList));
		}
		return result;
	}
}
