package jadx.core.dex.nodes;

public enum ProcessState {
	NOT_LOADED,
	LOADED,
	PROCESS_STARTED,
	PROCESS_COMPLETE,
	GENERATED;

	public boolean isProcessed() {
		return this == PROCESS_COMPLETE || this == GENERATED;
	}
}
