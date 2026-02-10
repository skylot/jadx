package jadx.gui.strings.pkg;

import jadx.api.JavaPackage;

public class StringPackageFilter extends PackageFilter {

	private String pkgName;

	public StringPackageFilter(final String configuration, final PackageMatchType matchType) {
		super(configuration, matchType);
	}

	@Override
	public void commonBeforeAll(final JavaPackage pkg) {
		this.pkgName = pkg.getFullName();
	}

	@Override
	public boolean contains(JavaPackage pkg) {
		return this.pkgName.contains(getConfiguration());
	}

	@Override
	public boolean startsWith(JavaPackage pkg) {
		return this.pkgName.indexOf(getConfiguration()) == 0;
	}

	@Override
	public boolean is(JavaPackage pkg) {
		return this.pkgName.equals(getConfiguration());
	}
}
