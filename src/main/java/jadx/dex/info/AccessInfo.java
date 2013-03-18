package jadx.dex.info;

import jadx.Consts;

import com.android.dx.rop.code.AccessFlags;

public class AccessInfo {

	private final int accFlags;

	public static enum AFType {
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
		if (containsFlag(flag))
			return new AccessInfo(accFlags - flag, type);
		else
			return this;
	}

	public AccessInfo getVisibility() {
		int f = (accFlags & AccessFlags.ACC_PUBLIC)
				| (accFlags & AccessFlags.ACC_PROTECTED)
				| (accFlags & AccessFlags.ACC_PRIVATE);
		return new AccessInfo(f, type);
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

	public int getFlags() {
		return accFlags;
	}

	public String makeString() {
		StringBuilder code = new StringBuilder();
		if ((accFlags & AccessFlags.ACC_PUBLIC) != 0)
			code.append("public ");

		if ((accFlags & AccessFlags.ACC_PRIVATE) != 0)
			code.append("private ");

		if ((accFlags & AccessFlags.ACC_PROTECTED) != 0)
			code.append("protected ");

		if (isStatic())
			code.append("static ");

		if (isFinal())
			code.append("final ");

		if (isAbstract())
			code.append("abstract ");

		if (isNative())
			code.append("native ");

		switch (type) {
			case METHOD:
				if ((accFlags & AccessFlags.ACC_SYNCHRONIZED) != 0)
					code.append("synchronized ");

				if ((accFlags & AccessFlags.ACC_DECLARED_SYNCHRONIZED) != 0)
					code.append("synchronized ");

				if (isBridge())
					code.append("/* bridge */ ");

				if (Consts.DEBUG) {
					if (isVarArgs())
						code.append("/* varargs */ ");
				}
				break;

			case FIELD:
				if ((accFlags & AccessFlags.ACC_VOLATILE) != 0)
					code.append("volatile ");

				if ((accFlags & AccessFlags.ACC_TRANSIENT) != 0)
					code.append("transient ");
				break;

			case CLASS:
				if ((accFlags & AccessFlags.ACC_STRICT) != 0)
					code.append("strict ");

				if (Consts.DEBUG) {
					if ((accFlags & AccessFlags.ACC_SUPER) != 0)
						code.append("/* super */ ");

					if ((accFlags & AccessFlags.ACC_ENUM) != 0)
						code.append("/* enum */ ");
				}
				break;
		}

		if (isSynthetic())
			code.append("/* synthetic */ ");

		return code.toString();
	}

}
