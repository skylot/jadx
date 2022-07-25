package jadx.core.dex.nodes;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.core.dex.info.PackageInfo;

public class PackageNode implements IPackageUpdate, IDexNode, Comparable<PackageNode> {

	private final RootNode root;
	private final PackageInfo pkgInfo;
	private final @Nullable PackageNode parentPkg;
	private final List<PackageNode> subPackages = new ArrayList<>();
	private final List<ClassNode> classes = new ArrayList<>();

	private PackageInfo aliasPkgInfo;

	public static PackageNode getForClass(RootNode root, String fullPkg, ClassNode cls) {
		PackageNode pkg = getOrBuild(root, fullPkg);
		pkg.getClasses().add(cls);
		return pkg;
	}

	public static PackageNode getOrBuild(RootNode root, String fullPkg) {
		PackageNode existPkg = root.resolvePackage(fullPkg);
		if (existPkg != null) {
			return existPkg;
		}
		PackageInfo pgkInfo = PackageInfo.fromFullPkg(root, fullPkg);
		PackageNode parentPkg = getParentPkg(root, pgkInfo);
		PackageNode pkgNode = new PackageNode(root, parentPkg, pgkInfo);
		if (parentPkg != null) {
			parentPkg.getSubPackages().add(pkgNode);
		}
		root.addPackage(pkgNode);
		return pkgNode;
	}

	private static @Nullable PackageNode getParentPkg(RootNode root, PackageInfo pgkInfo) {
		return root.resolvePackage(pgkInfo.getParentPkg());
	}

	private PackageNode(RootNode root, @Nullable PackageNode parentPkg, PackageInfo pkgInfo) {
		this.root = root;
		this.parentPkg = parentPkg;
		this.pkgInfo = pkgInfo;
	}

	public void rename(String alias) {
		rename(alias, true);
	}

	public void rename(String alias, boolean runUpdates) {
		if (pkgInfo.getName().equals(alias)) {
			aliasPkgInfo = pkgInfo;
			return;
		}
		aliasPkgInfo = PackageInfo.fromShortName(root, getParentAliasPkgInfo(), alias);
		if (runUpdates) {
			updatePackages(this);
		}
	}

	@Override
	public void onParentPackageUpdate(PackageNode updatedPkg) {
		aliasPkgInfo = PackageInfo.fromShortName(root, getParentAliasPkgInfo(), aliasPkgInfo.getName());
		updatePackages(updatedPkg);
	}

	public void updatePackages() {
		updatePackages(this);
	}

	private void updatePackages(PackageNode updatedPkg) {
		for (PackageNode subPackage : subPackages) {
			subPackage.onParentPackageUpdate(updatedPkg);
		}
		for (ClassNode cls : classes) {
			cls.onParentPackageUpdate(updatedPkg);
		}
	}

	public PackageInfo getPkgInfo() {
		return pkgInfo;
	}

	public PackageInfo getAliasPkgInfo() {
		return aliasPkgInfo;
	}

	public PackageNode getParentPkg() {
		return parentPkg;
	}

	public @Nullable PackageInfo getParentAliasPkgInfo() {
		return parentPkg == null ? null : parentPkg.aliasPkgInfo;
	}

	public List<PackageNode> getSubPackages() {
		return subPackages;
	}

	public List<ClassNode> getClasses() {
		return classes;
	}

	@Override
	public String typeName() {
		return "package";
	}

	@Override
	public RootNode root() {
		return root;
	}

	@Override
	public String getInputFileName() {
		return "";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof PackageNode)) {
			return false;
		}
		return pkgInfo.equals(((PackageNode) o).pkgInfo);
	}

	@Override
	public int hashCode() {
		return pkgInfo.hashCode();
	}

	@Override
	public int compareTo(@NotNull PackageNode other) {
		return getPkgInfo().getFullName().compareTo(other.getPkgInfo().getFullName());
	}

	@Override
	public String toString() {
		return getPkgInfo().getFullName();
	}
}
