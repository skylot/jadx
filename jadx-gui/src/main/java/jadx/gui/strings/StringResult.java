package jadx.gui.strings;

import java.util.List;

import org.jetbrains.kotlin.com.google.common.base.Objects;

import jadx.gui.strings.pkg.PackageFilter;
import jadx.gui.treemodel.JNode;

public abstract class StringResult extends JNode {

	public static void mergeStringResults(final StringResult newResult, final StringResult otherResult, final int otherResultIndex,
			final List<StringResult> results) {
		if (otherResult instanceof SingleStringResult) {
			final StringResultGrouping grouping =
					new StringResultGrouping(newResult.getRepresentativeString(), List.of(otherResult, newResult));
			results.remove(otherResultIndex);
			results.add(otherResultIndex, grouping);
		} else if (otherResult instanceof StringResultGrouping) {
			final StringResultGrouping grouping = (StringResultGrouping) otherResult;
			grouping.addResult(newResult);
		} else {
			throw new ClassCastException("Unsure how to add new string result to type " + newResult.getClass().getSimpleName());
		}
	}

	private final String string;

	public StringResult(final String string) {
		this.string = string;
	}

	public abstract boolean isIncludedForPackageFilters(final List<PackageFilter> packageFilters);

	@Override
	public String toString() {
		return getRepresentativeString();
	}

	@Override
	public String makeString() {
		return getRepresentativeString();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof StringResult)) {
			return false;
		}

		final StringResult other = (StringResult) obj;
		return getRepresentativeString().equals(other.getRepresentativeString());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(string, this.getClass());
	}

	@Override
	public int compareTo(final JNode other) {
		if (other instanceof StringResult) {
			final StringResult otherStringResult = (StringResult) other;
			return getRepresentativeString().compareTo(otherStringResult.getRepresentativeString());
		}

		return super.compareTo(other);
	}

	public String getRepresentativeString() {
		return this.string;
	}
}
