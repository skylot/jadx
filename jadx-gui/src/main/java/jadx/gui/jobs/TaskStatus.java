package jadx.gui.jobs;

import java.util.Set;

public enum TaskStatus {
	WAIT,
	STARTED,
	COMPLETE,
	CANCEL_BY_USER,
	CANCEL_BY_TIMEOUT,
	CANCEL_BY_MEMORY,
	ERROR;

	public static TaskStatus merge(Set<TaskStatus> statuses) {
		if (statuses.size() == 1) {
			return statuses.iterator().next();
		}
		if (statuses.contains(TaskStatus.CANCEL_BY_MEMORY)) {
			return TaskStatus.CANCEL_BY_MEMORY;
		}
		if (statuses.contains(TaskStatus.CANCEL_BY_TIMEOUT)) {
			return TaskStatus.CANCEL_BY_TIMEOUT;
		}
		if (statuses.contains(TaskStatus.CANCEL_BY_USER)) {
			return TaskStatus.CANCEL_BY_USER;
		}
		return TaskStatus.COMPLETE;
	}
}
