package jadx.gui.jobs;

public enum TaskStatus {
	WAIT,
	STARTED,
	COMPLETE,
	CANCEL_BY_USER,
	CANCEL_BY_TIMEOUT,
	CANCEL_BY_MEMORY,
	ERROR;
}
