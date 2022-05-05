package jadx.gui.jobs;

public interface Cancelable {
	boolean isCanceled();

	void cancel();
}
