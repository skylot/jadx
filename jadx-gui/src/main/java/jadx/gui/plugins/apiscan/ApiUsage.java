package jadx.gui.plugins.apiscan;

import java.util.Objects;

import jadx.core.dex.nodes.MethodNode;

public final class ApiUsage {
	private final MethodNode mth;
	private final ApiCategory category;
	private final String api;

	public ApiUsage(MethodNode mth, ApiCategory category, String api) {
		this.mth = mth;
		this.category = category;
		this.api = api;
	}

	public MethodNode getMth() {
		return mth;
	}

	public ApiCategory getCategory() {
		return category;
	}

	public String getApi() {
		return api;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ApiUsage)) {
			return false;
		}
		ApiUsage other = (ApiUsage) o;
		return mth.equals(other.mth) && category == other.category && api.equals(other.api);
	}

	@Override
	public int hashCode() {
		return Objects.hash(mth, category, api);
	}

	@Override
	public String toString() {
		return category + ": " + api + " in " + mth;
	}
}
