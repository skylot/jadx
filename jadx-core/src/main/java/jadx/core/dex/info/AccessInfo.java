package jadx.core.dex.info;

import jadx.core.Consts;

import com.android.dx.rop.code.AccessFlags;

public class AccessInfo {

	private final int accFlags;

	public enum AFType {
		CLASS, FIELD, METHOD
	}

	private final AFType type;

	public AccessInfo(int accessFlags, AFType type) {
		this.accFlags = accessFlags;
		this.type = type;
	}

	public boolean containsFlag(int flag) {
		return (accFlags & flag) != 0;
	}

	public AccessInfo remove(int flag) {
		if (containsFlag(flag)) {
			return new AccessInfo(accFlags & ~flag, type);
		}
		return this;
	}

	public AccessInfo getVisibility() {
		int f = accFlags & AccessFlags.ACC_PUBLIC
				| accFlags & AccessFlags.ACC_PROTECTED
				| accFlags & AccessFlags.ACC_PRIVATE;
		return new AccessInfo(f, type);
	}

	public boolean isPublic() {
		return (accFlags & AccessFlags.ACC_PUBLIC) != 0;
	}

	public boolean isProtected() {
		return (accFlags & AccessFlags.ACC_PROTECTED) != 0;
	}

	public boolean isPrivate() {
		return (accFlags & AccessFlags.ACC_PRIVATE) != 0;
	}

	public boolean isAbstract() {
		return (accFlags & AccessFlags.ACC_ABSTRACT) != 0;
	}

	public boolean isInterface() {
		return (accFlags & AccessFlags.ACC_INTERFACE) != 0;
	}

	public boolean isAnnotation() {
		return (accFlags & AccessFlags.ACC_ANNOTATION) != 0;
	}

	public boolean isNative() {
		return (accFlags & AccessFlags.ACC_NATIVE) != 0;
	}

	public boolean isStatic() {
		return (accFlags & AccessFlags.ACC_STATIC) != 0;
	}

	public boolean isFinal() {
		return (accFlags & AccessFlags.ACC_FINAL) != 0;
	}

	public boolean isConstructor() {
		return (accFlags & AccessFlags.ACC_CONSTRUCTOR) != 0;
	}

	public boolean isEnum() {
		return (accFlags & AccessFlags.ACC_ENUM) != 0;
	}

	public boolean isSynthetic() {
		return (accFlags & AccessFlags.ACC_SYNTHETIC) != 0;
	}

	public boolean isBridge() {
		return (accFlags & AccessFlags.ACC_BRIDGE) != 0;
	}

	public boolean isVarArgs() {
		return (accFlags & AccessFlags.ACC_VARARGS) != 0;
	}

	public boolean isSynchronized() {
		return (accFlags & (AccessFlags.ACC_SYNCHRONIZED | AccessFlags.ACC_DECLARED_SYNCHRONIZED)) != 0;
	}

	public boolean isTransient() {
		return (accFlags & AccessFlags.ACC_TRANSIENT) != 0;
	}

	public boolean isVolatile() {
		return (accFlags & AccessFlags.ACC_VOLATILE) != 0;
	}

	public AFType getType() {
		return type;
	}

	public String makeString() {
		StringBuilder code = new StringBuilder();
		if (isPublic()) {
			code.append("public ");
		}
		if (isPrivate()) {
			code.append("private ");
		}
		if (isProtected()) {
			code.append("protected ");
		}
		if (isStatic()) {
			code.append("static ");
		}
		if (isFinal()) {
			code.append("final ");
		}
		if (isAbstract()) {
			code.append("abstract ");
		}
		if (isNative()) {
			code.append("native ");
		}
		switch (type) {
			case METHOD:
				if (isSynchronized()) {
					code.append("synchronized ");
				}
				if (isBridge()) {
					code.append("/* bridge */ ");
				}
				if (Consts.DEBUG) {
					if (isVarArgs()) {
						code.append("/* varargs */ ");
					}
				}
				break;

			case FIELD:
				if (isVolatile()) {
					code.append("volatile ");
				}
				if (isTransient()) {
					code.append("transient ");
				}
				break;

			case CLASS:
				if ((accFlags & AccessFlags.ACC_STRICT) != 0) {
					code.append("strict ");
				}
				if (Consts.DEBUG) {
					if ((accFlags & AccessFlags.ACC_SUPER) != 0) {
						code.append("/* super */ ");
					}
					if ((accFlags & AccessFlags.ACC_ENUM) != 0) {
						code.append("/* enum */ ");
					}
				}
				break;
		}
		if (isSynthetic()) {
			code.append("/* synthetic */ ");
		}
		return code.toString();
	}

	public String rawString() {
		switch (type) {
			case CLASS:
				return AccessFlags.classString(accFlags);
			case FIELD:
				return AccessFlags.fieldString(accFlags);
			case METHOD:
				return AccessFlags.methodString(accFlags);
			default:
				return "?";
		}
	}

	@Override
	public String toString() {
		return "AccessInfo: " + type + " 0x" + Integer.toHexString(accFlags) + " (" + rawString() + ")";
	}
}
