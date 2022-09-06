package jadx.gui.jobs;

public interface Cancelable {
	boolean isCanceled();

	void cancel();

	default int getCancelTimeoutMS() {
		return 2000;
	}

	default int getShutdownTimeoutMS() {
		return 5000;
	}
}
