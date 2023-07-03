package jadx.core.dex.info;

import org.jetbrains.annotations.Nullable;

import jadx.core.utils.StringUtils;

class ClassAliasInfo {
	private final String shortName;
	@Nullable
	private final String pkg;
	@Nullable
	private String fullName;

	ClassAliasInfo(@Nullable String pkg, String shortName) {
		if (StringUtils.isEmpty(shortName)) {
			throw new IllegalArgumentException("Class alias can't be empty");
		}
		this.pkg = pkg;
		this.shortName = shortName;
	}

	@Nullable
	public String getPkg() {
		return pkg;
	}

	public String getShortName() {
		return shortName;
	}

	@Nullable
	public String getFullName() {
		return fullName;
	}

	public void setFullName(@Nullable String fullName) {
		this.fullName = fullName;
	}

	@Override
	public String toString() {
		return "Alias{" + shortName + ", pkg=" + pkg + ", fullName=" + fullName + '}';
	}
}
