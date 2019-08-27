package jadx.core.dex.nodes;

public enum ProcessState {
	NOT_LOADED,
	LOADED,
	PROCESS_STARTED,
	PROCESS_COMPLETE,
	GENERATED,
	UNLOADED;

	public boolean isLoaded() {
		return this != NOT_LOADED;
	}

	public boolean isProcessed() {
		return this == PROCESS_COMPLETE || this == GENERATED || this == UNLOADED;
	}
}
