package jadx.core;

public class Consts {
	public static final boolean DEBUG = false;
	public static final boolean DEBUG_WITH_ERRORS = false; // TODO: fix errors
	public static final boolean DEBUG_USAGE = false;
	public static final boolean DEBUG_TYPE_INFERENCE = false;
	public static final boolean DEBUG_OVERLOADED_CASTS = false;
	public static final boolean DEBUG_EXC_HANDLERS = false;
	public static final boolean DEBUG_FINALLY = false;
	public static final boolean DEBUG_ATTRIBUTES = false;

	public static final String CLASS_OBJECT = "java.lang.Object";
	public static final String CLASS_STRING = "java.lang.String";
	public static final String CLASS_CLASS = "java.lang.Class";
	public static final String CLASS_THROWABLE = "java.lang.Throwable";
	public static final String CLASS_EXCEPTION = "java.lang.Exception";
	public static final String CLASS_ENUM = "java.lang.Enum";

	public static final String CLASS_STRING_BUILDER = "java.lang.StringBuilder";
	public static final String OVERRIDE_ANNOTATION = "Ljava/lang/Override;";

	public static final String DEFAULT_PACKAGE_NAME = "defpackage";
	public static final String ANONYMOUS_CLASS_PREFIX = "AnonymousClass";

	public static final String MTH_TOSTRING_SIGNATURE = "toString()Ljava/lang/String;";

	private Consts() {
	}
}
