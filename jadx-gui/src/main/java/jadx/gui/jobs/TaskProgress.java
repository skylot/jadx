package jadx.gui.jobs;

import jadx.gui.utils.UiUtils;

public class TaskProgress implements ITaskProgress {
	private int progress;
	private int total;

	public TaskProgress() {
		this(0, 100);
	}

	public TaskProgress(long progress, long total) {
		this(UiUtils.calcProgress(progress, total), 100);
	}

	public TaskProgress(int progress, int total) {
		this.progress = progress;
		this.total = total;
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
