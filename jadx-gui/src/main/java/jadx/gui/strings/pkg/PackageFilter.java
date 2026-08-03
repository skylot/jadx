package jadx.gui.strings.pkg;

import java.util.Objects;

import jadx.api.JavaPackage;

public abstract class PackageFilter extends PackageMatchResolver {

	private final PackageMatchType matchType;
	private final String configuration;

	public PackageFilter(final String configuration, final PackageMatchType matchType) {
		this.matchType = matchType;
		this.configuration = configuration;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(getMatchType().toString());
		sb.append(' ');
		sb.append(getConfiguration());
		return sb.toString();
	}

	public final boolean doesMatch(final JavaPackage pkg) {
		commonBeforeAll(pkg);
		return getMatchType().doesPackageMatchFilter(this, pkg);
	}

	public final PackageMatchType getMatchType() {
		return this.matchType;
	}

	public final String getConfiguration() {
		return this.configuration;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null || !(obj instanceof PackageFilter)) {
			return false;
		}

		final PackageFilter other = (PackageFilter) obj;
		return getMatchType().equals(other.getMatchType()) && getConfiguration().equals(other.getConfiguration());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getMatchType(), getConfiguration());
	}
}
