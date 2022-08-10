package jadx.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import jadx.api.metadata.ICodeAnnotation;
import jadx.core.dex.info.PackageInfo;
import jadx.core.dex.nodes.PackageNode;

public final class JavaPackage implements JavaNode, Comparable<JavaPackage> {
	private final PackageNode pkgNode;
	private final List<JavaClass> classes;
	private final List<JavaPackage> subPkgs;

	JavaPackage(PackageNode pkgNode, List<JavaClass> classes, List<JavaPackage> subPkgs) {
		this.pkgNode = pkgNode;
		this.classes = classes;
		this.subPkgs = subPkgs;
	}

	@Override
	public String getName() {
		return pkgNode.getAliasPkgInfo().getName();
	}

	@Override
	public String getFullName() {
		return pkgNode.getAliasPkgInfo().getFullName();
	}

	public String getRawName() {
		return pkgNode.getPkgInfo().getName();
	}

	public String getRawFullName() {
		return pkgNode.getPkgInfo().getFullName();
	}

	public List<JavaPackage> getSubPackages() {
		return subPkgs;
	}

	public List<JavaClass> getClasses() {
		return classes;
	}

	public boolean isRoot() {
		return pkgNode.isRoot();
	}

	public boolean isLeaf() {
		return pkgNode.isLeaf();
	}

	public boolean isDefault() {
		return getFullName().isEmpty();
	}

	public void rename(String alias) {
		pkgNode.rename(alias);
	}

	@Override
	public void removeAlias() {
		pkgNode.removeAlias();
	}

	public boolean isParentRenamed() {
		PackageInfo parent = pkgNode.getPkgInfo().getParentPkg();
		PackageInfo aliasParent = pkgNode.getAliasPkgInfo().getParentPkg();
		return !Objects.equals(parent, aliasParent);
	}

	@Internal
	public PackageNode getPkgNode() {
		return pkgNode;
	}

	@Override
	public JavaClass getDeclaringClass() {
		return null;
	}

	@Override
	public JavaClass getTopParentClass() {
		return null;
	}

	@Override
	public int getDefPos() {
		return 0;
	}

	@Override
	public List<JavaNode> getUseIn() {
		List<JavaNode> list = new ArrayList<>();
		addUseIn(list);
		return list;
	}

	public void addUseIn(List<JavaNode> list) {
		list.addAll(classes);
		for (JavaPackage subPkg : subPkgs) {
			subPkg.addUseIn(list);
		}
	}

	@Override
	public boolean isOwnCodeAnnotation(ICodeAnnotation ann) {
		return false;
	}

	@Override
	public int compareTo(@NotNull JavaPackage o) {
		return pkgNode.compareTo(o.pkgNode);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		JavaPackage that = (JavaPackage) o;
		return pkgNode.equals(that.pkgNode);
	}

	@Override
	public int hashCode() {
		return pkgNode.hashCode();
	}

	@Override
	public String toString() {
		return pkgNode.toString();
	}
}
