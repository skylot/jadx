package jadx.core.xmlgen;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.RootNode;

/*
 * Modifies android:name attributes and xml tags which were changed during deobfuscation
 */
public class XmlDeobf {

	private XmlDeobf() {
	}

	@Nullable
	public static String deobfClassName(RootNode root, String potentialClassName, String packageName) {
		if (potentialClassName.indexOf('.') == -1) {
			return null;
		}
		if (packageName != null && potentialClassName.startsWith(".")) {
			potentialClassName = packageName + potentialClassName;
		}
		ArgType clsType = ArgType.object(potentialClassName);
		ClassInfo classInfo = root.getInfoStorage().getCls(clsType);
		if (classInfo == null) {
			// unknown class reference
			return null;
		}
		return classInfo.getAliasFullName();
	}
}
