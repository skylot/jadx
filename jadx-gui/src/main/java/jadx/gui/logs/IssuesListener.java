package jadx.gui.logs;

import javax.swing.SwingUtilities;

import ch.qos.logback.classic.Level;

import jadx.gui.ui.panel.IssuesPanel;
import jadx.gui.utils.rx.DebounceUpdate;

public class IssuesListener implements ILogListener {
	private final IssuesPanel issuesPanel;
	private final DebounceUpdate updater;

	private int errors = 0;
	private int warnings = 0;

	public IssuesListener(IssuesPanel issuesPanel) {
		this.issuesPanel = issuesPanel;
		this.updater = new DebounceUpdate(500, this::onUpdate);
	}

	private void onUpdate() {
		SwingUtilities.invokeLater(() -> issuesPanel.onUpdate(errors, warnings));
	}

	@Override
	public void onAppend(LogEvent logEvent) {
		switch (logEvent.getLevel().toInt()) {
			case Level.ERROR_INT:
				errors++;
				updater.requestUpdate();
				break;

			case Level.WARN_INT:
				warnings++;
				updater.requestUpdate();
				break;
		}
	}

	@Override
	public void onReload() {
		errors = 0;
		warnings = 0;
		updater.requestUpdate();
	}

	public int getErrors() {
		return errors;
	}

	public int getWarnings() {
		return warnings;
	}
}
