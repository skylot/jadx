package jadx.plugins.input.java.data.attributes;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.annotations.AnnotationVisibility;
import jadx.plugins.input.java.data.attributes.debuginfo.LineNumberTableAttr;
import jadx.plugins.input.java.data.attributes.debuginfo.LocalVarTypesAttr;
import jadx.plugins.input.java.data.attributes.debuginfo.LocalVarsAttr;
import jadx.plugins.input.java.data.attributes.types.CodeAttr;
import jadx.plugins.input.java.data.attributes.types.ConstValueAttr;
import jadx.plugins.input.java.data.attributes.types.IgnoredAttr;
import jadx.plugins.input.java.data.attributes.types.JavaAnnotationDefaultAttr;
import jadx.plugins.input.java.data.attributes.types.JavaAnnotationsAttr;
import jadx.plugins.input.java.data.attributes.types.JavaBootstrapMethodsAttr;
import jadx.plugins.input.java.data.attributes.types.JavaExceptionsAttr;
import jadx.plugins.input.java.data.attributes.types.JavaInnerClsAttr;
import jadx.plugins.input.java.data.attributes.types.JavaMethodParametersAttr;
import jadx.plugins.input.java.data.attributes.types.JavaParamAnnsAttr;
import jadx.plugins.input.java.data.attributes.types.JavaSignatureAttr;
import jadx.plugins.input.java.data.attributes.types.JavaSourceFileAttr;

public final class JavaAttrType<T extends IJavaAttribute> {

	private static final Map<String, JavaAttrType<?>> NAME_TO_TYPE_MAP;

	public static final JavaAttrType<JavaInnerClsAttr> INNER_CLASSES;
	public static final JavaAttrType<JavaBootstrapMethodsAttr> BOOTSTRAP_METHODS;

	public static final JavaAttrType<ConstValueAttr> CONST_VALUE;

	public static final JavaAttrType<CodeAttr> CODE;
	public static final JavaAttrType<LineNumberTableAttr> LINE_NUMBER_TABLE;
	public static final JavaAttrType<LocalVarsAttr> LOCAL_VAR_TABLE;
	public static final JavaAttrType<LocalVarTypesAttr> LOCAL_VAR_TYPE_TABLE;

	public static final JavaAttrType<JavaAnnotationsAttr> RUNTIME_ANNOTATIONS;
	public static final JavaAttrType<JavaAnnotationsAttr> BUILD_ANNOTATIONS;
	public static final JavaAttrType<JavaParamAnnsAttr> RUNTIME_PARAMETER_ANNOTATIONS;
	public static final JavaAttrType<JavaParamAnnsAttr> BUILD_PARAMETER_ANNOTATIONS;
	public static final JavaAttrType<IgnoredAttr> RUNTIME_TYPE_ANNOTATIONS;
	public static final JavaAttrType<IgnoredAttr> BUILD_TYPE_ANNOTATIONS;
	public static final JavaAttrType<JavaAnnotationDefaultAttr> ANNOTATION_DEFAULT;

	public static final JavaAttrType<JavaSourceFileAttr> SOURCE_FILE;
	public static final JavaAttrType<JavaSignatureAttr> SIGNATURE;
	public static final JavaAttrType<JavaExceptionsAttr> EXCEPTIONS;
	public static final JavaAttrType<JavaMethodParametersAttr> METHOD_PARAMETERS;

	public static final JavaAttrType<IgnoredAttr> DEPRECATED;
	public static final JavaAttrType<IgnoredAttr> SYNTHETIC;
	public static final JavaAttrType<IgnoredAttr> STACK_MAP_TABLE;
	public static final JavaAttrType<IgnoredAttr> ENCLOSING_METHOD;
	public static final JavaAttrType<IgnoredAttr> MODULE;

	static {
		NAME_TO_TYPE_MAP = new HashMap<>();

		CONST_VALUE = bind("ConstantValue", ConstValueAttr.reader());

		CODE = bind("Code", CodeAttr.reader());

		LINE_NUMBER_TABLE = bind("LineNumberTable", LineNumberTableAttr.reader());
		LOCAL_VAR_TABLE = bind("LocalVariableTable", LocalVarsAttr.reader());
		LOCAL_VAR_TYPE_TABLE = bind("LocalVariableTypeTable", LocalVarTypesAttr.reader());

		INNER_CLASSES = bind("InnerClasses", JavaInnerClsAttr.reader());
		BOOTSTRAP_METHODS = bind("BootstrapMethods", JavaBootstrapMethodsAttr.reader());

		RUNTIME_ANNOTATIONS = bind("RuntimeVisibleAnnotations", JavaAnnotationsAttr.reader(AnnotationVisibility.RUNTIME));
		BUILD_ANNOTATIONS = bind("RuntimeInvisibleAnnotations", JavaAnnotationsAttr.reader(AnnotationVisibility.BUILD));
		RUNTIME_PARAMETER_ANNOTATIONS = bind("RuntimeVisibleParameterAnnotations", JavaParamAnnsAttr.reader(AnnotationVisibility.RUNTIME));
		BUILD_PARAMETER_ANNOTATIONS = bind("RuntimeInvisibleParameterAnnotations", JavaParamAnnsAttr.reader(AnnotationVisibility.BUILD));
		ANNOTATION_DEFAULT = bind("AnnotationDefault", JavaAnnotationDefaultAttr.reader());

		SOURCE_FILE = bind("SourceFile", JavaSourceFileAttr.reader());
		SIGNATURE = bind("Signature", JavaSignatureAttr.reader());
		EXCEPTIONS = bind("Exceptions", JavaExceptionsAttr.reader());
		METHOD_PARAMETERS = bind("MethodParameters", JavaMethodParametersAttr.reader());

		// ignored
		DEPRECATED = bind("Deprecated", null); // duplicated by annotation
		SYNTHETIC = bind("Synthetic", null); // duplicated by access flag
		STACK_MAP_TABLE = bind("StackMapTable", null);
		ENCLOSING_METHOD = bind("EnclosingMethod", null);

		// TODO: not supported yet
		RUNTIME_TYPE_ANNOTATIONS = bind("RuntimeVisibleTypeAnnotations", null);
		BUILD_TYPE_ANNOTATIONS = bind("RuntimeInvisibleTypeAnnotations", null);
		MODULE = bind("Module", null);
	}

	private static <A extends IJavaAttribute> JavaAttrType<A> bind(String name, IJavaAttributeReader reader) {
		JavaAttrType<A> attrType = new JavaAttrType<>(NAME_TO_TYPE_MAP.size(), name, reader);
		NAME_TO_TYPE_MAP.put(name, attrType);
		return attrType;
	}

	@Nullable
	public static JavaAttrType<?> byName(String name) {
		return NAME_TO_TYPE_MAP.get(name);
	}

	public static int size() {
		return NAME_TO_TYPE_MAP.size();
	}

	private final int id;
	private final String name;
	private final IJavaAttributeReader reader;

	private JavaAttrType(int id, String name, IJavaAttributeReader reader) {
		this.id = id;
		this.name = name;
		this.reader = reader;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public IJavaAttributeReader getReader() {
		return reader;
	}

	@Override
	public String toString() {
		return name;
	}
}
