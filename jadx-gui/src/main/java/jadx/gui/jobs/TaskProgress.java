package jadx.gui.jobs;

public class TaskProgress implements ITaskProgress {

	private int progress;
	private int total;

	public TaskProgress() {
	}

	public TaskProgress(int progress, int total) {
		this.progress = progress;
		this.total = total;
	}

	public TaskProgress(long progress, long total) {
		// TODO: apply normalization to fit into int
		this.progress = (int) progress;
		this.total = (int) total;
	}

	@Override
	public int progress() {
		return progress;
	}

	@Override
	public int total() {
		return total;
	}

	public void updateProgress(int progress) {
		this.progress = progress;
	}

	public void updateTotal(int total) {
		this.total = total;
	}
}
