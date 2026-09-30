package jadx.gui.links;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.MethodInfo;

/**
 * Find class or method referenced by {@link JadxLink} in loaded code
 */
public class JadxLinkResolver {
	private final JadxDecompiler decompiler;

	public JadxLinkResolver(JadxDecompiler decompiler) {
		this.decompiler = decompiler;
	}

	public @Nullable JavaNode resolve(JadxLink link) {
		List<JavaClass> classes = decompiler.getClassesWithInners();
		String mthName = link.getMthName();
		if (mthName == null) {
			JavaClass cls = findClass(classes, link.getClsName());
			if (cls != null) {
				return cls;
			}
			// 'pkg.Cls.method' without parentheses
			int sep = link.getClsName().lastIndexOf('.');
			if (sep <= 0) {
				return null;
			}
			cls = findClass(classes, link.getClsName().substring(0, sep));
			return cls == null ? null : findMethod(cls, link.getClsName().substring(sep + 1), link);
		}
		JavaClass cls = findClass(classes, link.getClsName());
		return cls == null ? null : findMethod(cls, mthName, link);
	}

	private static @Nullable JavaClass findClass(List<JavaClass> classes, String name) {
		// prefer original names: stable between users, not affected by renames and deobfuscation
		for (JavaClass cls : classes) {
			ClassInfo clsInfo = cls.getClassNode().getClassInfo();
			if (clsInfo.getFullName().equals(name) || clsInfo.makeRawFullName().equals(name)) {
				return cls;
			}
		}
		for (JavaClass cls : classes) {
			if (cls.getFullName().equals(name) || cls.getRawName().equals(name)) {
				return cls;
			}
		}
		return null;
	}

	private static @Nullable JavaMethod findMethod(JavaClass cls, String name, JadxLink link) {
		List<JavaMethod> methods = cls.getMethods();
		JavaMethod firstByName = null;
		for (JavaMethod mth : methods) {
			MethodInfo mthInfo = mth.getMethodNode().getMethodInfo();
			if (mthInfo.getName().equals(name) || mth.getName().equals(name)) {
				if (link.argsMatch(mthInfo.getArgumentsTypes())) {
					return mth;
				}
				if (firstByName == null) {
					firstByName = mth;
				}
			}
		}
		return firstByName;
	}
}
