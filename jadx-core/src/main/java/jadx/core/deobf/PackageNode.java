package jadx.core.deobf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class PackageNode {

	private static final char SEPARATOR_CHAR = '.';

	private PackageNode parentPackage;
	private List<PackageNode> innerPackages = Collections.emptyList();

	private final String packageName;
	private String packageAlias;

	private String cachedPackageFullName;
	private String cachedPackageFullAlias;

	public PackageNode(String packageName) {
		this.packageName = packageName;
		this.parentPackage = this;
	}

	public String getName() {
		return packageName;
	}

	public String getFullName() {
		if (cachedPackageFullName == null) {
			Stack<PackageNode> pp = getParentPackages();

			StringBuilder result = new StringBuilder();
			result.append(pp.pop().getName());
			while (pp.size() > 0) {
				result.append(SEPARATOR_CHAR);
				result.append(pp.pop().getName());
			}
			cachedPackageFullName = result.toString();
		}
		return cachedPackageFullName;
	}

	public String getAlias() {
		if (packageAlias != null) {
			return packageAlias;
		}
		return packageName;
	}

	public void setAlias(String alias) {
		packageAlias = alias;
	}

	public boolean hasAlias() {
		return packageAlias != null;
	}

	public String getFullAlias() {
		if (cachedPackageFullAlias == null) {
			Stack<PackageNode> pp = getParentPackages();
			StringBuilder result = new StringBuilder();

			if (pp.size() > 0) {
				result.append(pp.pop().getAlias());
				while (pp.size() > 0) {
					result.append(SEPARATOR_CHAR);
					result.append(pp.pop().getAlias());
				}
			} else {
				result.append(this.getAlias());
			}
			cachedPackageFullAlias = result.toString();
		}
		return cachedPackageFullAlias;
	}

	public PackageNode getParentPackage() {
		return parentPackage;
	}

	public List<PackageNode> getInnerPackages() {
		return innerPackages;
	}

	public void addInnerPackage(PackageNode pkg) {
		if (innerPackages.isEmpty()) {
			innerPackages = new ArrayList<PackageNode>();
		}
		innerPackages.add(pkg);
		pkg.parentPackage = this;
	}

	/**
	 * Gets inner package node by name
	 *
	 * @param name inner package name
	 * @return package node or {@code null}
	 */
	public PackageNode getInnerPackageByName(String name) {
		PackageNode result = null;
		for (PackageNode p : innerPackages) {
			if (p.getName().equals(name)) {
				result = p;
				break;
			}
		}
		return result;
	}

	/**
	 * Fills stack with parent packages exclude root node
	 *
	 * @return stack with parent packages
	 */
	private Stack<PackageNode> getParentPackages() {
		Stack<PackageNode> pp = new Stack<PackageNode>();

		PackageNode currentP = this;
		PackageNode parentP = currentP.getParentPackage();

		while (currentP != parentP) {
			pp.push(currentP);
			currentP = parentP;
			parentP = currentP.getParentPackage();
		}
		return pp;
	}
}
