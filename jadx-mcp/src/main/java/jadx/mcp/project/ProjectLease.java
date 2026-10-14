package jadx.mcp.project;

import jadx.api.JadxDecompiler;
import jadx.mcp.tools.CancellationToken;

/**
 * Access to decompiler of one project generation for the duration of a tool call.
 * Must be closed after use. Do not store decompiler outside the lease scope.
 */
public final class ProjectLease implements AutoCloseable {
	private final ProjectGate gate;
	private final JadxDecompiler decompiler;
	private final long generation;
	private final CancellationToken token;

	ProjectLease(ProjectGate gate, JadxDecompiler decompiler, long generation, CancellationToken token) {
		this.gate = gate;
		this.decompiler = decompiler;
		this.generation = generation;
		this.token = token;
	}

	public JadxDecompiler getDecompiler() {
		token.throwIfCancelled();
		return decompiler;
	}

	public long getGeneration() {
		return generation;
	}

	public CancellationToken getToken() {
		return token;
	}

	/**
	 * Cancellation checkpoint: throws if call cancelled, timed out or project closing
	 */
	public void check() {
		token.throwIfCancelled();
	}

	@Override
	public void close() {
		gate.release(this);
	}
}
