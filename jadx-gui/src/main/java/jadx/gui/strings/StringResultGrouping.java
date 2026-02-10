package jadx.gui.strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;

import jadx.gui.strings.pkg.PackageFilter;
import jadx.gui.treemodel.JClass;

public final class StringResultGrouping extends StringResult {

	private final List<StringResult> results;

	private boolean expanded = false;

	public StringResultGrouping(final String string, final List<StringResult> results) {
		super(string);

		this.results = new ArrayList<>(results);
	}

	@Override
	public boolean isIncludedForPackageFilters(final List<PackageFilter> packageFilters) {
		boolean matched = false;
		for (final StringResult result : getResults()) {
			if (result.isIncludedForPackageFilters(packageFilters)) {
				matched = true;
				break;
			}
		}
		return matched;
	}

	@Override
	public Icon getIcon() {
		return null;
	}

	@Override
	public JClass getJParent() {
		return null;
	}

	public void addResult(final StringResult newResult) {
		this.results.add(newResult);
	}

	public int getCount() {
		return this.results.size();
	}

	public int getCount(final List<PackageFilter> packageFilters) {
		final List<StringResult> results = getResults(packageFilters);
		return results.size();
	}

	public List<StringResult> getResults() {
		return Collections.unmodifiableList(this.results);
	}

	public List<StringResult> getResults(final List<PackageFilter> packageFilters) {
		final List<StringResult> results = getResults();
		final List<StringResult> filteredResults = new ArrayList<>(results.size());

		for (final StringResult result : results) {
			if (!result.isIncludedForPackageFilters(packageFilters)) {
				continue;
			}

			filteredResults.add(result);
		}

		return Collections.unmodifiableList(filteredResults);
	}

	public boolean isExpanded() {
		return this.expanded;
	}

	public void setExpanded(final boolean expanded) {
		this.expanded = expanded;
	}
}
