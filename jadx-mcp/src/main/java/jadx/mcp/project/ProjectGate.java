package jadx.mcp.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxDecompiler;
import jadx.mcp.tools.CancellationToken;
import jadx.mcp.tools.ToolErrorCode;
import jadx.mcp.tools.ToolException;

/**
 * Guard decompiler access for MCP requests.
 * <br>
 * Requests take a lease on current project generation. On project close all leases are cancelled
 * and close waits (bounded) until running requests release the decompiler,
 * so requests never use a closed decompiler instance.
 */
public final class ProjectGate {
	private static final Logger LOG = LoggerFactory.getLogger(ProjectGate.class);

	public enum State {
		EMPTY,
		LOADING,
		READY,
		CLOSING
	}

	private final Set<ProjectLease> leases = ConcurrentHashMap.newKeySet();
	private State state = State.EMPTY;
	private long generation;
	private @Nullable JadxDecompiler decompiler;

	public synchronized State getState() {
		return state;
	}

	/**
	 * Incremented on each project load. Zero if project never loaded.
	 */
	public synchronized long getGeneration() {
		return generation;
	}

	public synchronized void setLoading() {
		if (state == State.READY) {
			LOG.warn("MCP project gate: loading started without close");
		}
		state = State.LOADING;
		decompiler = null;
	}

	public synchronized void setReady(JadxDecompiler loadedDecompiler) {
		decompiler = loadedDecompiler;
		generation++;
		state = State.READY;
	}

	/**
	 * Reset to empty state if loading failed or cancelled
	 */
	public synchronized void setEmpty() {
		if (!leases.isEmpty()) {
			LOG.warn("MCP project gate: reset with {} active leases", leases.size());
		}
		decompiler = null;
		state = State.EMPTY;
	}

	/**
	 * Reject new requests, cancel running and wait for release.
	 * Should not be called on UI thread: requests can wait for UI updates.
	 *
	 * @return true if all leases released before timeout
	 */
	public boolean close(long waitMs) {
		List<ProjectLease> active;
		synchronized (this) {
			if (state == State.EMPTY) {
				return true;
			}
			state = State.CLOSING;
			decompiler = null;
			active = new ArrayList<>(leases);
		}
		for (ProjectLease lease : active) {
			lease.getToken().cancel(CancellationToken.Reason.PROJECT_CLOSED);
		}
		boolean released = waitForRelease(waitMs);
		if (!released) {
			LOG.warn("MCP project gate: {} requests still running after close timeout", leases.size());
		}
		synchronized (this) {
			state = State.EMPTY;
		}
		return released;
	}

	private synchronized boolean waitForRelease(long waitMs) {
		long deadline = System.currentTimeMillis() + waitMs;
		while (!leases.isEmpty()) {
			long remaining = deadline - System.currentTimeMillis();
			if (remaining <= 0) {
				return false;
			}
			try {
				wait(remaining);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
		}
		return true;
	}

	public synchronized ProjectLease acquire(CancellationToken token) throws ToolException {
		switch (state) {
			case EMPTY:
				throw new ToolException(ToolErrorCode.PROJECT_NOT_LOADED,
						"No project loaded in jadx. Open an APK/DEX/JAR file in jadx-gui first.");
			case LOADING:
				throw new ToolException(ToolErrorCode.PROJECT_LOADING,
						"Project is loading, retry in a few seconds.");
			case CLOSING:
				throw new ToolException(ToolErrorCode.PROJECT_CLOSING,
						"Project is closing or reloading, retry later.");
			case READY:
			default:
				break;
		}
		JadxDecompiler current = decompiler;
		if (current == null) {
			throw new ToolException(ToolErrorCode.PROJECT_NOT_LOADED, "No project loaded");
		}
		ProjectLease lease = new ProjectLease(this, current, generation, token);
		leases.add(lease);
		return lease;
	}

	synchronized void release(ProjectLease lease) {
		if (leases.remove(lease)) {
			notifyAll();
		}
	}

	public int getActiveLeasesCount() {
		return leases.size();
	}
}
