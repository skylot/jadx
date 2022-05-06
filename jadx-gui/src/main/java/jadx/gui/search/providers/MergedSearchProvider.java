package jadx.gui.search.providers;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.gui.jobs.Cancelable;
import jadx.gui.search.ISearchProvider;
import jadx.gui.treemodel.JNode;

/**
 * Search provider for sequential execution of nested search providers
 */
public class MergedSearchProvider implements ISearchProvider {

	private final List<ISearchProvider> list = new ArrayList<>();
	private int current;
	private int total;

	public void add(ISearchProvider provider) {
		list.add(provider);
	}

	public void prepare() {
		current = list.isEmpty() ? -1 : 0;
		total = list.stream().mapToInt(ISearchProvider::total).sum();
	}

	@Override
	public @Nullable JNode next(Cancelable cancelable) {
		if (current == -1) {
			return null;
		}
		while (true) {
			JNode next = list.get(current).next(cancelable);
			if (next != null) {
				return next;
			}
			current++;
			if (current >= list.size() || cancelable.isCanceled()) {
				// search complete
				current = -1;
				return null;
			}
		}
	}

	@Override
	public int progress() {
		return list.stream().mapToInt(ISearchProvider::progress).sum();
	}

	@Override
	public int total() {
		return total;
	}
}
