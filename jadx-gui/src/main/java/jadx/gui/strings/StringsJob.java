package jadx.gui.strings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.strings.providers.StringsProviderDelegate;

public class StringsJob implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(StringsJob.class);

	private final StringsTask stringsTask;
	private final StringsProviderDelegate provider;

	public StringsJob(final StringsTask task, final StringsProviderDelegate provider) {
		this.stringsTask = task;
		this.provider = provider;
	}

	@Override
	public void run() {
		while (true) {
			try {
				final StringResult result = provider.next(stringsTask);
				if (result == null) {
					break;
				}
				if (stringsTask.addResult(result)) {
					break;
				}
			} catch (final Exception e) {
				LOG.warn("Strings error, provider: {}", provider.getClass().getSimpleName(), e);
				break;
			}
		}
	}

	public StringsProviderDelegate getDelegate() {
		return this.provider;
	}
}
