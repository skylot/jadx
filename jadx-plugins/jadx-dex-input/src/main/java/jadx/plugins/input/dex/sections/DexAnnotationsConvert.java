package jadx.plugins.input.dex.sections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.input.data.annotations.AnnotationVisibility;
import jadx.api.plugins.input.data.annotations.EncodedType;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.annotations.IAnnotation;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.api.plugins.input.data.attributes.types.AnnotationDefaultClassAttr;
import jadx.api.plugins.input.data.attributes.types.AnnotationsAttr;
import jadx.api.plugins.input.data.attributes.types.ExceptionsAttr;
import jadx.api.plugins.input.data.attributes.types.InnerClassesAttr;
import jadx.api.plugins.input.data.attributes.types.InnerClsInfo;
import jadx.api.plugins.input.data.attributes.types.MethodParametersAttr;
import jadx.api.plugins.input.data.attributes.types.SignatureAttr;
import jadx.api.plugins.utils.Utils;
import jadx.plugins.input.dex.sections.annotations.AnnotationsUtils;

public class DexAnnotationsConvert {
	private static final Logger LOG = LoggerFactory.getLogger(DexAnnotationsConvert.class);

	public static void forClass(String cls, List<IJadxAttribute> list, List<IAnnotation> annotationList) {
		appendAnnotations(cls, list, annotationList);
	}

	public static void forMethod(List<IJadxAttribute> list, List<IAnnotation> annotationList) {
		appendAnnotations(null, list, annotationList);
	}

	public static void forField(List<IJadxAttribute> list, List<IAnnotation> annotationList) {
		appendAnnotations(null, list, annotationList);
	}

	private static void appendAnnotations(@Nullable String cls, List<IJadxAttribute> attributes, List<IAnnotation> annotations) {
		if (annotations.isEmpty()) {
			return;
		}
		for (IAnnotation annotation : annotations) {
			if (annotation.getVisibility() == AnnotationVisibility.SYSTEM) {
				convertSystemAnnotations(cls, attributes, annotation);
			}
		}
		Utils.addToList(attributes, AnnotationsAttr.pack(annotations));
	}

	@SuppressWarnings("unchecked")
	private static void convertSystemAnnotations(@Nullable String cls, List<IJadxAttribute> attributes, IAnnotation annotation) {
		switch (annotation.getAnnotationClass()) {
			case "Ldalvik/annotation/Signature;":
				attributes.add(new SignatureAttr(extractSignature(annotation)));
				break;

			case "Ldalvik/annotation/InnerClass;":
				try {
					String name = AnnotationsUtils.getValue(annotation, "name", EncodedType.ENCODED_STRING, null);
					int accFlags = AnnotationsUtils.getValue(annotation, "accessFlags", EncodedType.ENCODED_INT, 0);
					if (name != null || accFlags != 0) {
						InnerClsInfo innerClsInfo = new InnerClsInfo(cls, null, name, accFlags);
						attributes.add(new InnerClassesAttr(Collections.singletonMap(cls, innerClsInfo)));
					}
				} catch (Exception e) {
					LOG.warn("Failed to parse annotation: " + annotation, e);
				}
				break;

			case "Ldalvik/annotation/AnnotationDefault;":
				EncodedValue annValue = annotation.getDefaultValue();
				if (annValue != null && annValue.getType() == EncodedType.ENCODED_ANNOTATION) {
					IAnnotation defAnnotation = (IAnnotation) annValue.getValue();
					attributes.add(new AnnotationDefaultClassAttr(defAnnotation.getValues()));
				}
				break;

			case "Ldalvik/annotation/Throws;":
				try {
					EncodedValue defaultValue = annotation.getDefaultValue();
					if (defaultValue != null) {
						List<String> excs = ((List<EncodedValue>) defaultValue.getValue())
								.stream()
								.map(ev -> ((String) ev.getValue()))
								.collect(Collectors.toList());
						attributes.add(new ExceptionsAttr(excs));
					}
				} catch (Exception e) {
					LOG.warn("Failed to convert dalvik throws annotation", e);
				}
				break;

			case "Ldalvik/annotation/MethodParameters;":
				try {
					List<EncodedValue> names = AnnotationsUtils.getArray(annotation, "names");
					List<EncodedValue> accFlags = AnnotationsUtils.getArray(annotation, "accessFlags");
					if (!names.isEmpty() && names.size() == accFlags.size()) {
						int size = names.size();
						List<MethodParametersAttr.Info> list = new ArrayList<>(size);
						for (int i = 0; i < size; i++) {
							String name = (String) names.get(i).getValue();
							int accFlag = (int) accFlags.get(i).getValue();
							list.add(new MethodParametersAttr.Info(accFlag, name));
						}
						attributes.add(new MethodParametersAttr(list));
					}
				} catch (Exception e) {
					LOG.warn("Failed to parse annotation: " + annotation, e);
				}
				break;
		}
	}

	@SuppressWarnings({ "unchecked", "ConstantConditions" })
	private static String extractSignature(IAnnotation annotation) {
		List<EncodedValue> values = (List<EncodedValue>) annotation.getDefaultValue().getValue();
		if (values.size() == 1) {
			return (String) values.get(0).getValue();
		}
		StringBuilder sb = new StringBuilder();
		for (EncodedValue part : values) {
			sb.append((String) part.getValue());
		}
		return sb.toString();
	}
}
