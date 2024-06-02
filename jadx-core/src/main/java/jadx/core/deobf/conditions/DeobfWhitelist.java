package jadx.core.deobf.conditions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.PackageNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.Utils;

public class DeobfWhitelist extends AbstractDeobfCondition {
	private static final Logger LOG = LoggerFactory.getLogger(DeobfWhitelist.class);

	public static final List<String> DEFAULT_LIST = Arrays.asList(
			"android.support.*",
			"android.os.*",
			"androidx.core.os.*",
			"androidx.annotation.*");

	public static final String DEFAULT_STR = Utils.listToString(DEFAULT_LIST, " ");

	private final Set<String> packages = new HashSet<>();
	private final Set<ClassNode> classes = new HashSet<>();
	private boolean reportMissingItems = false;

	@Override
	public void init(RootNode root) {
		packages.clear();
		classes.clear();
		List<String> excludeList = root.getArgs().getDeobfuscationWhitelist();
		reportMissingItems = !excludeList.equals(DEFAULT_LIST);
		for (String name : excludeList) {
			if (name.isEmpty()) {
				continue;
			}
			if (name.endsWith(".*")) {
				excludePackage(root, name.substring(0, name.length() - 2));
			} else {
				excludeClass(root, name);
			}
		}
		LOG.debug("Excluded from deobfuscation: {} packages, {} classes", packages.size(), classes.size());
	}

	private void excludeClass(RootNode root, String clsFullName) {
		ClassNode cls = root.resolveClass(clsFullName);
		if (cls == null) {
			if (reportMissingItems) {
				LOG.info("Can't exclude from deobfuscation: class '{}' not found", clsFullName);
			}
			return;
		}
		excludeClsNode(cls);
	}

	private void excludeClsNode(ClassNode cls) {
		classes.add(cls);
		cls.addInfoComment("Class excluded from deobfuscation");
	}

	private void excludePackage(RootNode root, String fullPkgName) {
		PackageNode pkg = root.resolvePackage(fullPkgName);
		if (pkg == null) {
			if (reportMissingItems) {
				LOG.info("Can't exclude from deobfuscation: package '{}' not found", fullPkgName);
			}
			return;
		}
		excludePkgNode(pkg);
	}

	private void excludePkgNode(PackageNode pkg) {
		packages.add(pkg.getFullName());
		pkg.getClasses().forEach(this::excludeClsNode);
		pkg.getSubPackages().forEach(this::excludePkgNode);
	}

	@Override
	public Action check(PackageNode pkg) {
		if (packages.contains(pkg.getPkgInfo().getFullName())) {
			return Action.FORBID_RENAME;
		}
		return Action.NO_ACTION;
	}

	@Override
	public Action check(ClassNode cls) {
		if (classes.contains(cls)) {
			return Action.FORBID_RENAME;
		}
		return Action.NO_ACTION;
	}

	@Override
	public Action check(FieldNode fld) {
		return check(fld.getParentClass());
	}

	@Override
	public Action check(MethodNode mth) {
		return check(mth.getParentClass());
	}
}
