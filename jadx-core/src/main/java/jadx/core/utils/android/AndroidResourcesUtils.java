package jadx.core.utils.android;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.codegen.ClassGen;
import jadx.core.codegen.CodeWriter;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.RootNode;

/**
 * Android resources specific handlers
 */
public class AndroidResourcesUtils {
	private static final Logger LOG = LoggerFactory.getLogger(AndroidResourcesUtils.class);

	private AndroidResourcesUtils() {
	}

	public static ClassNode searchAppResClass(RootNode root) {
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
		return makeClass(root, fullName);
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

	private static ClassNode makeClass(RootNode root, String clsName) {
		List<DexNode> dexNodes = root.getDexNodes();
		if (dexNodes.isEmpty()) {
			return null;
		}
		ClassInfo r = ClassInfo.fromName(root, clsName);
		return new ClassNode(dexNodes.get(0), r);
	}
}
