package jadx.api.plugins.input.data;

public class AccessFlags {
	public static final int PUBLIC = 0x1;
	public static final int PRIVATE = 0x2;
	public static final int PROTECTED = 0x4;
	public static final int STATIC = 0x8;
	public static final int FINAL = 0x10;
	public static final int SYNCHRONIZED = 0x20;
	public static final int SUPER = 0x20;
	public static final int VOLATILE = 0x40;
	public static final int BRIDGE = 0x40;
	public static final int TRANSIENT = 0x80;
	public static final int VARARGS = 0x80;
	public static final int NATIVE = 0x100;
	public static final int INTERFACE = 0x200;
	public static final int ABSTRACT = 0x400;
	public static final int STRICT = 0x800;
	public static final int SYNTHETIC = 0x1000;
	public static final int ANNOTATION = 0x2000;
	public static final int ENUM = 0x4000;
	public static final int MODULE = 0x8000;
	public static final int CONSTRUCTOR = 0x10000;
	public static final int DECLARED_SYNCHRONIZED = 0x20000;

	public static boolean hasFlag(int flags, int flagValue) {
		return (flags & flagValue) != 0;
	}

	public static String format(int flags, AccessFlagsScope scope) {
		StringBuilder code = new StringBuilder();
		if (hasFlag(flags, PUBLIC)) {
			code.append("public ");
		}
		if (hasFlag(flags, PRIVATE)) {
			code.append("private ");
		}
		if (hasFlag(flags, PROTECTED)) {
			code.append("protected ");
		}
		if (hasFlag(flags, STATIC)) {
			code.append("static ");
		}
		if (hasFlag(flags, FINAL)) {
			code.append("final ");
		}
		if (hasFlag(flags, ABSTRACT)) {
			code.append("abstract ");
		}
		if (hasFlag(flags, NATIVE)) {
			code.append("native ");
		}
		switch (scope) {
			case METHOD:
				if (hasFlag(flags, SYNCHRONIZED)) {
					code.append("synchronized ");
				}
				if (hasFlag(flags, BRIDGE)) {
					code.append("bridge ");
				}
				if (hasFlag(flags, VARARGS)) {
					code.append("varargs ");
				}
				break;

			case FIELD:
				if (hasFlag(flags, VOLATILE)) {
					code.append("volatile ");
				}
				if (hasFlag(flags, TRANSIENT)) {
					code.append("transient ");
				}
				break;

			case CLASS:
				if (hasFlag(flags, MODULE)) {
					code.append("module ");
				}
				if (hasFlag(flags, STRICT)) {
					code.append("strict ");
				}
				if (hasFlag(flags, SUPER)) {
					code.append("super ");
				}
				if (hasFlag(flags, ENUM)) {
					code.append("enum ");
				}
				break;
		}
		if (hasFlag(flags, SYNTHETIC)) {
			code.append("synthetic ");
		}
		return code.toString();
	}
}
