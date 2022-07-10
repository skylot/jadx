package jadx.gui.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.treemodel.JNode;

public class SearchJob implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(SearchJob.class);
	private final SearchTask searchTask;
	private final ISearchProvider provider;

	public SearchJob(SearchTask task, ISearchProvider provider) {
		this.searchTask = task;
		this.provider = provider;
	}

	@Override
	public void run() {
		while (true) {
			try {
				JNode result = provider.next(searchTask);
				if (result == null) {
					return;
				}
				if (searchTask.addResult(result)) {
					return;
				}
			} catch (Exception e) {
				LOG.warn("Search error, provider: {}", provider.getClass().getSimpleName(), e);
				return;
			}
		}
	}

	public ISearchProvider getProvider() {
		return provider;
	}
}
