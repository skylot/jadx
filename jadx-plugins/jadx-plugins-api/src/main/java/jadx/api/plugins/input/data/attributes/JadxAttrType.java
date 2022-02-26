package jadx.api.plugins.input.data.attributes;

import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.attributes.types.AnnotationDefaultAttr;
import jadx.api.plugins.input.data.attributes.types.AnnotationDefaultClassAttr;
import jadx.api.plugins.input.data.attributes.types.AnnotationMethodParamsAttr;
import jadx.api.plugins.input.data.attributes.types.AnnotationsAttr;
import jadx.api.plugins.input.data.attributes.types.ExceptionsAttr;
import jadx.api.plugins.input.data.attributes.types.InnerClassesAttr;
import jadx.api.plugins.input.data.attributes.types.MethodParametersAttr;
import jadx.api.plugins.input.data.attributes.types.SignatureAttr;
import jadx.api.plugins.input.data.attributes.types.SourceFileAttr;

public final class JadxAttrType<T extends IJadxAttribute> implements IJadxAttrType<T> {

	// class, method, field
	public static final JadxAttrType<AnnotationsAttr> ANNOTATION_LIST = bind();
	public static final JadxAttrType<SignatureAttr> SIGNATURE = bind();

	// class
	public static final JadxAttrType<SourceFileAttr> SOURCE_FILE = bind();
	public static final JadxAttrType<InnerClassesAttr> INNER_CLASSES = bind();
	public static final JadxAttrType<AnnotationDefaultClassAttr> ANNOTATION_DEFAULT_CLASS = bind(); // dex specific

	// field
	public static final JadxAttrType<EncodedValue> CONSTANT_VALUE = bind();

	// method
	public static final JadxAttrType<AnnotationMethodParamsAttr> ANNOTATION_MTH_PARAMETERS = bind();
	public static final JadxAttrType<AnnotationDefaultAttr> ANNOTATION_DEFAULT = bind();
	public static final JadxAttrType<ExceptionsAttr> EXCEPTIONS = bind();
	public static final JadxAttrType<MethodParametersAttr> METHOD_PARAMETERS = bind();

	private static <T extends IJadxAttribute> JadxAttrType<T> bind() {
		return new JadxAttrType<>();
	}

	private JadxAttrType() {
	}
}
