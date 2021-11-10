package jadx.core.dex.nodes;

public enum ProcessState {
	NOT_LOADED,
	LOADED,
	PROCESS_STARTED,
	PROCESS_COMPLETE,
	GENERATED_AND_UNLOADED;

	public boolean isProcessComplete() {
		return this == PROCESS_COMPLETE || this == GENERATED_AND_UNLOADED;
	}
}
