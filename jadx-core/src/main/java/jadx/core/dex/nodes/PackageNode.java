package jadx.core.dex.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.api.JavaPackage;
import jadx.core.dex.info.PackageInfo;

import static jadx.core.utils.StringUtils.containsChar;

public class PackageNode implements IPackageUpdate, IDexNode, Comparable<PackageNode> {

	private final RootNode root;
	private final PackageInfo pkgInfo;
	private final @Nullable PackageNode parentPkg;
	private final List<PackageNode> subPackages = new ArrayList<>();
	private final List<ClassNode> classes = new ArrayList<>();

	private PackageInfo aliasPkgInfo;

	private JavaPackage javaNode;

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
		PackageInfo parentPkg = pgkInfo.getParentPkg();
		if (parentPkg == null) {
			return null;
		}
		return getOrBuild(root, parentPkg.getFullName());
	}

	private PackageNode(RootNode root, @Nullable PackageNode parentPkg, PackageInfo pkgInfo) {
		this.root = root;
		this.parentPkg = parentPkg;
		this.pkgInfo = pkgInfo;
		this.aliasPkgInfo = pkgInfo;
	}

	@Override
	public void rename(String newName) {
		rename(newName, true);
	}

	public void rename(String newName, boolean runUpdates) {
		String alias;
		boolean isFullAlias;
		if (containsChar(newName, '/')) {
			alias = newName.replace('/', '.');
			isFullAlias = true;
		} else if (newName.startsWith(".")) {
			// treat as full pkg, remove start dot
			alias = newName.substring(1);
			isFullAlias = true;
		} else {
			alias = newName;
			isFullAlias = containsChar(newName, '.');
		}
		if (isFullAlias) {
			setFullAlias(alias, runUpdates);
		} else {
			setLeafAlias(alias, runUpdates);
		}
	}

	public void setLeafAlias(String alias, boolean runUpdates) {
		if (pkgInfo.getName().equals(alias)) {
			aliasPkgInfo = pkgInfo;
		} else {
			aliasPkgInfo = PackageInfo.fromShortName(root, getParentAliasPkgInfo(), alias);
		}
		if (runUpdates) {
			updatePackages(this);
		}
	}

	public void setFullAlias(String fullAlias, boolean runUpdates) {
		if (pkgInfo.getFullName().equals(fullAlias)) {
			aliasPkgInfo = pkgInfo;
		} else {
			aliasPkgInfo = PackageInfo.fromFullPkg(root, fullAlias);
		}
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

	public boolean hasAlias() {
		if (pkgInfo == aliasPkgInfo) {
			return false;
		}
		return !pkgInfo.getName().equals(aliasPkgInfo.getName());
	}

	public boolean hasParentAlias() {
		if (pkgInfo == aliasPkgInfo) {
			return false;
		}
		return !Objects.equals(pkgInfo.getParentPkg(), aliasPkgInfo.getParentPkg());
	}

	public void removeAlias() {
		aliasPkgInfo = pkgInfo;
	}

	public @Nullable PackageNode getParentPkg() {
		return parentPkg;
	}

	public @Nullable PackageInfo getParentAliasPkgInfo() {
		return parentPkg == null ? null : parentPkg.aliasPkgInfo;
	}

	public boolean isRoot() {
		return parentPkg == null;
	}

	public boolean isLeaf() {
		return subPackages.isEmpty();
	}

	public List<PackageNode> getSubPackages() {
		return subPackages;
	}

	public List<ClassNode> getClasses() {
		return classes;
	}

	public JavaPackage getJavaNode() {
		return javaNode;
	}

	public void setJavaNode(JavaPackage javaNode) {
		this.javaNode = javaNode;
	}

	public boolean isEmpty() {
		return classes.isEmpty() && subPackages.isEmpty();
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
