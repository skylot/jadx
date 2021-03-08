package jadx.core.xmlgen;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;

/*
 * modifies android:name attributes and xml tags which are old class names
 * but were changed during deobfuscation
 */
public class XmlDeobf {
	private static final Map<String, String> DEOBF_MAP = new HashMap<>();

	private XmlDeobf() {
	}

	@Nullable
	public static String deobfClassName(RootNode rootNode, String potencialClassName, String packageName) {
		potencialClassName = potencialClassName.replace('$', '.');
		if (packageName != null && potencialClassName.startsWith(".")) {
			potencialClassName = packageName + potencialClassName;
		}
		return getNewClassName(rootNode, potencialClassName);
	}

	private static String getNewClassName(RootNode rootNode, String old) {
		if (DEOBF_MAP.isEmpty()) {
			for (ClassNode classNode : rootNode.getClasses(true)) {
				ClassInfo classInfo = classNode.getClassInfo();
				if (classInfo.hasAlias()) {
					String oldName = classInfo.getFullName();
					String newName = classInfo.getAliasFullName();
					if (!oldName.equals(newName)) {
						DEOBF_MAP.put(oldName, newName);
					}
				}
			}
		}
		return DEOBF_MAP.get(old);
	}
}
