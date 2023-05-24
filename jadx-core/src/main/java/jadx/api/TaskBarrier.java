package jadx.api;

import java.util.concurrent.CountDownLatch;

public class TaskBarrier {

	private CountDownLatch taskCountDown = null;

	public void setUpBarrier(final int numTasks) {
		taskCountDown = new CountDownLatch(numTasks);
	}

	public CountDownLatch getTaskCountDown() {
		return taskCountDown;
	}

	public void finishTask() {
		if (taskCountDown != null) {
			taskCountDown.countDown();
		}
	}
}
