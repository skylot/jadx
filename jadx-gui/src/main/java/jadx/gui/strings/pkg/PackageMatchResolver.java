package jadx.gui.strings.pkg;

import jadx.api.JavaPackage;

public abstract class PackageMatchResolver {

	public abstract boolean contains(final JavaPackage pkg);

	public abstract boolean startsWith(final JavaPackage pkg);

	public abstract boolean is(final JavaPackage pkg);

	/**
	 * An optionally overridable method to prepare data common to all rule type implementations.
	 *
	 * @param pkg The JavaPackage being evaluated.
	 */
	public void commonBeforeAll(final JavaPackage pkg) {
		// do nothing
	}
}
