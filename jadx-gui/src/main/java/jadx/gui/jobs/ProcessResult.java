package jadx.gui.jobs;

public class ProcessResult {
	private final int skipped;
	private final TaskStatus status;
	private final int timeLimit;

	public ProcessResult(int skipped, TaskStatus status, int timeLimit) {
		this.skipped = skipped;
		this.status = status;
		this.timeLimit = timeLimit;
	}

	public int getSkipped() {
		return skipped;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public int getTimeLimit() {
		return timeLimit;
	}
}
