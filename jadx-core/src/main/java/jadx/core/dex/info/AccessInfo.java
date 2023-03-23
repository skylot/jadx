package jadx.core.dex.info;

import org.intellij.lang.annotations.MagicConstant;

import jadx.api.plugins.input.data.AccessFlags;
import jadx.core.Consts;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class AccessInfo {

	public static final int VISIBILITY_FLAGS = AccessFlags.PUBLIC | AccessFlags.PROTECTED | AccessFlags.PRIVATE;
	private final int accFlags;

	public enum AFType {
		CLASS, FIELD, METHOD
	}

	private final AFType type;

	public AccessInfo(int accessFlags, AFType type) {
		this.accFlags = accessFlags;
		this.type = type;
	}

	@MagicConstant(valuesFromClass = AccessFlags.class)
	public boolean containsFlag(int flag) {
		return (accFlags & flag) != 0;
	}

	@MagicConstant(valuesFromClass = AccessFlags.class)
	public boolean containsFlags(int... flags) {
		for (int flag : flags) {
			if ((accFlags & flag) == 0) {
				return false;
			}
		}
		return true;
	}

	public AccessInfo remove(int flag) {
		if (containsFlag(flag)) {
			return new AccessInfo(accFlags & ~flag, type);
		}
		return this;
	}

	public AccessInfo add(int flag) {
		if (!containsFlag(flag)) {
			return new AccessInfo(accFlags | flag, type);
		}
		return this;
	}

	public AccessInfo changeVisibility(int flag) {
		int currentVisFlags = accFlags & VISIBILITY_FLAGS;
		if (currentVisFlags == flag) {
			return this;
		}
		int unsetAllVisFlags = accFlags & ~VISIBILITY_FLAGS;
		return new AccessInfo(unsetAllVisFlags | flag, type);
	}

	public AccessInfo getVisibility() {
		return new AccessInfo(accFlags & VISIBILITY_FLAGS, type);
	}

	public boolean isVisibilityWeakerThan(AccessInfo otherAccInfo) {
		int thisVis = accFlags & VISIBILITY_FLAGS;
		int otherVis = otherAccInfo.accFlags & VISIBILITY_FLAGS;
		if (thisVis == otherVis) {
			return false;
		}
		return orderedVisibility(thisVis) < orderedVisibility(otherVis);
	}

	private static int orderedVisibility(int flag) {
		switch (flag) {
			case AccessFlags.PRIVATE:
				return 1;
			case 0: // package-private
				return 2;
			case AccessFlags.PROTECTED:
				return 3;
			case AccessFlags.PUBLIC:
				return 4;
			default:
				throw new JadxRuntimeException("Unexpected visibility flag: " + flag);
		}
	}

	public boolean isPublic() {
		return (accFlags & AccessFlags.PUBLIC) != 0;
	}

	public boolean isProtected() {
		return (accFlags & AccessFlags.PROTECTED) != 0;
	}

	public boolean isPrivate() {
		return (accFlags & AccessFlags.PRIVATE) != 0;
	}

	public boolean isPackagePrivate() {
		return (accFlags & VISIBILITY_FLAGS) == 0;
	}

	public boolean isAbstract() {
		return (accFlags & AccessFlags.ABSTRACT) != 0;
	}

	public boolean isInterface() {
		return (accFlags & AccessFlags.INTERFACE) != 0;
	}

	public boolean isAnnotation() {
		return (accFlags & AccessFlags.ANNOTATION) != 0;
	}

	public boolean isNative() {
		return (accFlags & AccessFlags.NATIVE) != 0;
	}

	public boolean isStatic() {
		return (accFlags & AccessFlags.STATIC) != 0;
	}

	public boolean isFinal() {
		return (accFlags & AccessFlags.FINAL) != 0;
	}

	public boolean isConstructor() {
		return (accFlags & AccessFlags.CONSTRUCTOR) != 0;
	}

	public boolean isEnum() {
		return (accFlags & AccessFlags.ENUM) != 0;
	}

	public boolean isSynthetic() {
		return (accFlags & AccessFlags.SYNTHETIC) != 0;
	}

	public boolean isBridge() {
		return (accFlags & AccessFlags.BRIDGE) != 0;
	}

	public boolean isVarArgs() {
		return (accFlags & AccessFlags.VARARGS) != 0;
	}

	public boolean isSynchronized() {
		return (accFlags & (AccessFlags.SYNCHRONIZED | AccessFlags.DECLARED_SYNCHRONIZED)) != 0;
	}

	public boolean isTransient() {
		return (accFlags & AccessFlags.TRANSIENT) != 0;
	}

	public boolean isVolatile() {
		return (accFlags & AccessFlags.VOLATILE) != 0;
	}

	public boolean isModuleInfo() {
		return (accFlags & AccessFlags.MODULE) != 0;
	}

	public AFType getType() {
		return type;
	}

	public String makeString(boolean showHidden) {
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
				if (showHidden) {
					if (isBridge()) {
						code.append("/* bridge */ ");
					}
					if (Consts.DEBUG && isVarArgs()) {
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
				if ((accFlags & AccessFlags.STRICT) != 0) {
					code.append("strict ");
				}
				if (showHidden) {
					if (isModuleInfo()) {
						code.append("/* module-info */ ");
					}
					if (Consts.DEBUG) {
						if ((accFlags & AccessFlags.SUPER) != 0) {
							code.append("/* super */ ");
						}
						if ((accFlags & AccessFlags.ENUM) != 0) {
							code.append("/* enum */ ");
						}
					}
				}
				break;
		}
		if (isSynthetic() && showHidden) {
			code.append("/* synthetic */ ");
		}
		return code.toString();
	}

	public String visibilityName() {
		if (isPackagePrivate()) {
			return "package-private";
		}
		if (isPublic()) {
			return "public";
		}
		if (isPrivate()) {
			return "private";
		}
		if (isProtected()) {
			return "protected";
		}
		throw new JadxRuntimeException("Unknown visibility flags: " + getVisibility());
	}

	public int rawValue() {
		return accFlags;
	}

	@Override
	public String toString() {
		return "AccessInfo: " + type + " 0x" + Integer.toHexString(accFlags) + " (" + makeString(true) + ')';
	}
}
