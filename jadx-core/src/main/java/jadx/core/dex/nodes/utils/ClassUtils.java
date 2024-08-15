package jadx.core.dex.nodes.utils;

import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class ClassUtils {

	private final RootNode root;

	public ClassUtils(RootNode rootNode) {
		this.root = rootNode;
	}

	public boolean isAccessible(ClassNode cls, ClassNode callerCls) {
		if (cls.equals(callerCls)) {
			return true;
		}

		final AccessInfo accessFlags = cls.getAccessFlags();
		if (accessFlags.isPublic()) {
			return true;
		}

		if (accessFlags.isProtected()) {
			return isProtectedAccessible(cls, callerCls);
		}

		if (accessFlags.isPackagePrivate()) {
			return isPackagePrivateAccessible(cls, callerCls);
		}

		if (accessFlags.isPrivate()) {
			return isPrivateAccessible(cls, callerCls);
		}

		throw new JadxRuntimeException(accessFlags + " is not supported");
	}

	private boolean isProtectedAccessible(ClassNode cls, ClassNode callerCls) {
		return isPackagePrivateAccessible(cls, callerCls) || isSuperType(cls, callerCls);
	}

	private boolean isPackagePrivateAccessible(ClassNode cls, ClassNode callerCls) {
		return cls.getPackageNode().equals(callerCls.getPackageNode());
	}

	private boolean isPrivateAccessible(ClassNode cls, ClassNode callerCls) {
		return cls.getTopParentClass().equals(callerCls.getTopParentClass());
	}

	private boolean isSuperType(ClassNode cls, ClassNode superCls) {
		return root.getClsp().getSuperTypes(cls.getRawName()).stream()
				.anyMatch(x -> x.equals(superCls.getRawName()));
	}
}
