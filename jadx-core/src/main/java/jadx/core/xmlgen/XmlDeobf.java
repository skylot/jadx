package jadx.core.xmlgen;

import java.util.HashMap;
import java.util.Map;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;

/*
 * modifies android:name attributes and xml tags which are old class names
 * but were changed during deobfuscation
 */
public class XmlDeobf {
	private static final Map<String, String> deobfMap = new HashMap<>();

	private XmlDeobf() {}

	public static String deobfClassName(RootNode rootNode, String potencialClassName,
	                                    String packageName) {

		if (packageName != null && potencialClassName.startsWith(".")) {
			potencialClassName = packageName + potencialClassName;
		}
		return getNewClassName(rootNode, potencialClassName);
	}

	private static String getNewClassName(RootNode rootNode, String old) {
		if (deobfMap.isEmpty()) {
			for (ClassNode classNode : rootNode.getClasses(true)) {
				if (classNode.getAlias() != null) {
					String oldName = classNode.getClassInfo().getFullName();
					String newName = classNode.getAlias().getFullName();
					if (!oldName.equals(newName)) {
						deobfMap.put(oldName, newName);
					}
				}
			}
		}
		return deobfMap.get(old);
	}
}
