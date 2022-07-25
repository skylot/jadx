package jadx.core.dex.info;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.nodes.RootNode;

public class PackageInfo {

	private final @Nullable PackageInfo parentPkg;
	private final String fullName;
	private final String name;

	public static synchronized PackageInfo fromFullPkg(RootNode root, String fullPkg) {
		PackageInfo existPkg = root.getInfoStorage().getPkg(fullPkg);
		if (existPkg != null) {
			return existPkg;
		}
		PackageInfo newPkg;
		int lastDot = fullPkg.lastIndexOf('.');
		if (lastDot == -1) {
			// unknown root pkg
			newPkg = new PackageInfo(fullPkg, null, fullPkg);
		} else {
			PackageInfo parentPkg = fromFullPkg(root, fullPkg.substring(0, lastDot));
			newPkg = new PackageInfo(fullPkg, parentPkg, fullPkg.substring(lastDot + 1));
		}
		root.getInfoStorage().putPkg(newPkg);
		return newPkg;
	}

	public static synchronized PackageInfo fromShortName(RootNode root, @Nullable PackageInfo parent, String shortName) {
		String fullPkg = parent == null ? shortName : parent.getFullName() + '.' + shortName;
		PackageInfo existPkg = root.getInfoStorage().getPkg(fullPkg);
		if (existPkg != null) {
			return existPkg;
		}
		PackageInfo newPkg = new PackageInfo(fullPkg, parent, shortName);
		root.getInfoStorage().putPkg(newPkg);
		return newPkg;
	}

	private PackageInfo(String fullName, @Nullable PackageInfo parentPkg, String name) {
		this.fullName = fullName;
		this.parentPkg = parentPkg;
		this.name = name;
	}

	public boolean isRoot() {
		return parentPkg == null;
	}

	public boolean isDefaultPkg() {
		return fullName.isEmpty();
	}

	public String getFullName() {
		return fullName;
	}

	public @Nullable PackageInfo getParentPkg() {
		return parentPkg;
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof PackageInfo)) {
			return false;
		}
		return Objects.equals(fullName, ((PackageInfo) o).getFullName());
	}

	@Override
	public int hashCode() {
		return fullName.hashCode();
	}

	@Override
	public String toString() {
		return fullName;
	}
}
