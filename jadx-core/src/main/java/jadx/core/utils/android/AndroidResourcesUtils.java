package jadx.core.utils.android;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.codegen.ClassGen;
import jadx.core.codegen.CodeWriter;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.xmlgen.ResourceStorage;
import jadx.core.xmlgen.entry.ResourceEntry;

/**
 * Android resources specific handlers
 */
public class AndroidResourcesUtils {
	private static final Logger LOG = LoggerFactory.getLogger(AndroidResourcesUtils.class);

	private AndroidResourcesUtils() {
	}

	public static ClassNode searchAppResClass(RootNode root, ResourceStorage resStorage) {
		String appPackage = root.getAppPackage();
		String fullName = appPackage != null ? appPackage + ".R" : "R";
		ClassNode resCls = root.searchClassByName(fullName);
		if (resCls != null) {
			return resCls;
		}
		List<ClassNode> candidates = root.searchClassByShortName("R");
		if (candidates.size() == 1) {
			return candidates.get(0);
		}
		if (!candidates.isEmpty()) {
			LOG.info("Found several 'R' class candidates: {}", candidates);
		}
		LOG.warn("Unknown 'R' class, create references to '{}'", fullName);
		return makeClass(root, fullName, resStorage);
	}

	public static boolean handleAppResField(CodeWriter code, ClassGen clsGen, ClassInfo declClass) {
		ClassInfo parentClass = declClass.getParentClass();
		if (parentClass != null && parentClass.getShortName().equals("R")) {
			clsGen.useClass(code, parentClass);
			code.add('.');
			code.add(declClass.getAlias().getShortName());
			return true;
		}
		return false;
	}

	private static ClassNode makeClass(RootNode root, String clsName, ResourceStorage resStorage) {
		List<DexNode> dexNodes = root.getDexNodes();
		if (dexNodes.isEmpty()) {
			return null;
		}
		ClassInfo r = ClassInfo.fromName(root, clsName);
		ClassNode classNode = new ClassNode(dexNodes.get(0), r);
		generateMissingRCode(classNode, resStorage);
		return classNode;
	}
	
	private static void generateMissingRCode(ClassNode cls, ResourceStorage resStorage) {
		Map<String, List<ResourceEntry>> sortedMap = new HashMap<>();
		for(ResourceEntry ri : resStorage.getResources()) {
			List<ResourceEntry> entries = sortedMap.get(ri.getTypeName());
			if(entries == null) {
				entries = new LinkedList<>();
				sortedMap.put(ri.getTypeName(), entries);
			}
			entries.add(ri);
		}
		
		Set<String> addedValues = new HashSet<>();
		CodeWriter clsCode = new CodeWriter();
		if (!"".equals(cls.getPackage())) {
			clsCode.add("package ").add(cls.getPackage()).add(';').newLine();
		}
		clsCode.startLine("public final class ").add(cls.getShortName()).add(" {").incIndent();
		for(String typeName : sortedMap.keySet()) {
			clsCode.startLine("public static final class ").add(typeName).add(" {").incIndent();
			for(ResourceEntry ri : sortedMap.get(typeName)) {
				if(addedValues.add(ri.getTypeName() + "." + ri.getKeyName())) {
					clsCode.startLine("public static final int ").add(ri.getKeyName()).add(" = ")
						.add("" + ri.getId()).add(";");
				}
			}
			clsCode.decIndent();
			clsCode.add("}");
		}
		clsCode.decIndent();
		clsCode.add("}");
		
		cls.setCode(clsCode);
	}
}
