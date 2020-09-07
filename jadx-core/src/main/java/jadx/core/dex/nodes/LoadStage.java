package jadx.core.dex.nodes;

public enum LoadStage {
	NONE,
	PROCESS_STAGE, // dependencies not yet loaded
	CODEGEN_STAGE, // all dependencies loaded
}
