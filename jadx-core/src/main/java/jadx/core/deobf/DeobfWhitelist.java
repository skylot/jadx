package jadx.core.deobf;

import java.util.ArrayList;
import java.util.List;

import jadx.api.JadxArgs;
import jadx.api.deobf.IRenameCondition;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.PackageNode;
import jadx.core.dex.nodes.RootNode;

public class DeobfWhitelist implements IRenameCondition {

	private static DeobfWhitelist whitelist = null;

	private final List<String> packages = new ArrayList<>();

	private final List<String> classes = new ArrayList<>();

	public static DeobfWhitelist getWhitelist() {
		if (whitelist == null) {
			whitelist = new DeobfWhitelist();
		}
		return whitelist;
	}

	@Override
	public void init(RootNode root) {
		packages.clear();
		classes.clear();
		JadxArgs args = root.getArgs();
		String whitelistStr = args.getDeobfuscationWhitelist();
		String[] whitelisteItems = whitelistStr.split(":");
		for (String whitelistItem : whitelisteItems) {
			if (!whitelistItem.isEmpty()) {
				if (whitelistItem.endsWith(".*")) {
					packages.add(whitelistItem.substring(0, whitelistItem.length() - 2));
				} else {
					classes.add(whitelistItem);
				}
			}
		}
	}

	@Override
	public boolean shouldRename(PackageNode pkg) {
		String fullname = pkg.getPkgInfo().getFullName();
		for (String p : packages) {
			if (fullname.equals(p)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean shouldRename(ClassNode cls) {
		String fullname = cls.getFullName();
		for (String c : classes) {
			if (fullname.equals(c)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean shouldRename(FieldNode fld) {
		return true;
	}

	@Override
	public boolean shouldRename(MethodNode mth) {
		return true;
	}
}
