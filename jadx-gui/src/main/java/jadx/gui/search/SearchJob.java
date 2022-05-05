package jadx.gui.search;

import jadx.gui.treemodel.JNode;

public class SearchJob implements Runnable {

	private final SearchTask searchTask;
	private final ISearchProvider provider;

	public SearchJob(SearchTask task, ISearchProvider provider) {
		this.searchTask = task;
		this.provider = provider;
	}

	@Override
	public void run() {
		while (true) {
			JNode result = provider.next(searchTask);
			if (result == null) {
				return;
			}
			if (searchTask.addResult(result)) {
				return;
			}
		}
	}

	public ISearchProvider getProvider() {
		return provider;
	}
}
