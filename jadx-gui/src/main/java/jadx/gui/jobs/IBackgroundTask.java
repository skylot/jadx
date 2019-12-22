package jadx.gui.jobs;

import java.util.List;

public interface IBackgroundTask {

	String getTitle();

	List<Runnable> scheduleJobs();

	void onFinish();

	boolean canBeCanceled();
}
