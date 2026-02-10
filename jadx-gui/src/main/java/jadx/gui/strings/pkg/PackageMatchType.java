package jadx.gui.strings.pkg;

import jadx.api.JavaPackage;
import jadx.gui.utils.NLS;

public enum PackageMatchType {
	CONTAINS((resolver, pkg) -> resolver.contains(pkg), NLS.str("strings.contains")),
	DOES_NOT_CONTAIN((resolver, pkg) -> !resolver.contains(pkg), NLS.str("strings.contains_not")),
	STARTS_WITH((resolver, pkg) -> resolver.startsWith(pkg), NLS.str("strings.starts_with")),
	DOES_NOT_START_WITH((resolver, pkg) -> !resolver.startsWith(pkg), NLS.str("strings.starts_with_not")),
	IS((resolver, pkg) -> resolver.is(pkg), NLS.str("strings.is")),
	IS_NOT((resolver, pkg) -> !resolver.is(pkg), NLS.str("strings.is_not"));

	private final String displayText;
	private final PackageFilterPredicate packageMatcher;

	private PackageMatchType(final PackageFilterPredicate packageMatcher, final String displayText) {
		this.displayText = displayText;
		this.packageMatcher = packageMatcher;
	}

	@Override
	public String toString() {
		return getDisplayText();
	}

	public String getDisplayText() {
		return this.displayText;
	}

	public boolean doesPackageMatchFilter(final PackageMatchResolver resolver, final JavaPackage pkg) {
		return this.packageMatcher.doesPackageMatchFilter(resolver, pkg);
	}

	/**
	 * A functional interface which determines whether a package matches the
	 * representative filter for a given package match resolver and package.
	 */
	@FunctionalInterface
	private interface PackageFilterPredicate {
		public abstract boolean doesPackageMatchFilter(PackageMatchResolver resolver, JavaPackage pkg);
	}
}
