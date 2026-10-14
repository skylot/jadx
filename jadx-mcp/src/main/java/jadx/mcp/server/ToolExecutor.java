package jadx.mcp.server;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import jadx.mcp.tools.CancellationToken;
import jadx.mcp.tools.ToolErrorCode;
import jadx.mcp.tools.ToolRegistry;

/**
 * Bounded pool for tool calls. Separated from HTTP threads, so a caller can stop waiting
 * on timeout or cancel while worker reaches the next cancellation checkpoint.
 */
public final class ToolExecutor {
	private static final Logger LOG = LoggerFactory.getLogger(ToolExecutor.class);

	private static final int QUEUE_SIZE = 64;

	private final ThreadPoolExecutor pool;

	public ToolExecutor(int threads) {
		AtomicInteger counter = new AtomicInteger();
		ThreadFactory threadFactory = r -> {
			Thread thread = new Thread(r, "jadx-mcp-tool-" + counter.incrementAndGet());
			thread.setDaemon(true);
			thread.setPriority(Thread.NORM_PRIORITY - 1);
			return thread;
		};
		this.pool = new ThreadPoolExecutor(threads, threads, 60, TimeUnit.SECONDS,
				new ArrayBlockingQueue<>(QUEUE_SIZE), threadFactory);
		this.pool.allowCoreThreadTimeOut(true);
	}

	/**
	 * Run task and wait for result, timeout or cancel.
	 */
	public JsonObject execute(Supplier<JsonObject> task, CancellationToken token, long timeoutMs) {
		Future<JsonObject> future;
		try {
			future = pool.submit(() -> {
				CancellationToken.Reason reason = token.getReason();
				if (reason != null) {
					return ToolRegistry.cancelled(reason);
				}
				return task.get();
			});
		} catch (RejectedExecutionException e) {
			return ToolRegistry.error(ToolErrorCode.BUSY, "Too many concurrent tool calls, retry later", null);
		}
		// worker threads are not interrupted: cooperative cancellation only
		token.onCancel(() -> future.cancel(false));
		try {
			if (timeoutMs > 0) {
				return future.get(timeoutMs, TimeUnit.MILLISECONDS);
			}
			return future.get();
		} catch (TimeoutException e) {
			token.cancel(CancellationToken.Reason.TIMEOUT);
			return ToolRegistry.cancelled(CancellationToken.Reason.TIMEOUT);
		} catch (CancellationException e) {
			CancellationToken.Reason reason = token.getReason();
			return ToolRegistry.cancelled(reason != null ? reason : CancellationToken.Reason.CLIENT_CANCELLED);
		} catch (InterruptedException e) {
			token.cancel(CancellationToken.Reason.SERVER_STOPPED);
			Thread.currentThread().interrupt();
			return ToolRegistry.cancelled(CancellationToken.Reason.SERVER_STOPPED);
		} catch (ExecutionException e) {
			LOG.warn("MCP tool execution failed", e.getCause());
			return ToolRegistry.error(ToolErrorCode.INTERNAL_ERROR, String.valueOf(e.getCause()), null);
		}
	}

	public int getActiveCount() {
		return pool.getActiveCount();
	}

	/**
	 * Stop accepting new calls. Running calls are not interrupted (they must be cancelled by tokens).
	 */
	public void shutdown() {
		pool.shutdown();
	}
}
