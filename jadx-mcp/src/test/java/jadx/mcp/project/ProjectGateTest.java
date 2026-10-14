package jadx.mcp.project;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.mcp.tools.CancellationToken;
import jadx.mcp.tools.ToolCancelledException;
import jadx.mcp.tools.ToolErrorCode;
import jadx.mcp.tools.ToolException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectGateTest {

	@Test
	void states() throws Exception {
		ProjectGate gate = new ProjectGate();
		assertCode(gate, ToolErrorCode.PROJECT_NOT_LOADED);
		gate.setLoading();
		assertCode(gate, ToolErrorCode.PROJECT_LOADING);
		try (JadxDecompiler decompiler = new JadxDecompiler(new JadxArgs())) {
			gate.setReady(decompiler);
			assertThat(gate.getGeneration()).isEqualTo(1);
			try (ProjectLease lease = gate.acquire(new CancellationToken())) {
				assertThat(lease.getDecompiler()).isSameAs(decompiler);
				assertThat(gate.getActiveLeasesCount()).isEqualTo(1);
			}
			assertThat(gate.getActiveLeasesCount()).isZero();
			assertThat(gate.close(1000)).isTrue();
			assertCode(gate, ToolErrorCode.PROJECT_NOT_LOADED);
			gate.setReady(decompiler);
			assertThat(gate.getGeneration()).isEqualTo(2);
		}
	}

	@Test
	void closeCancelsAndWaitsActiveLeases() throws Exception {
		ProjectGate gate = new ProjectGate();
		try (JadxDecompiler decompiler = new JadxDecompiler(new JadxArgs())) {
			gate.setReady(decompiler);
			CountDownLatch acquired = new CountDownLatch(1);
			CompletableFuture<String> worker = CompletableFuture.supplyAsync(() -> {
				try (ProjectLease lease = gate.acquire(new CancellationToken())) {
					acquired.countDown();
					while (true) {
						lease.check();
						Thread.sleep(5);
					}
				} catch (ToolCancelledException e) {
					return e.getReason().name();
				} catch (Exception e) {
					return "error: " + e;
				}
			});
			assertThat(acquired.await(5, TimeUnit.SECONDS)).isTrue();
			assertThat(gate.close(5000)).isTrue();
			assertThat(worker.get(5, TimeUnit.SECONDS)).isEqualTo("PROJECT_CLOSED");
			assertThat(gate.getActiveLeasesCount()).isZero();
		}
	}

	private static void assertCode(ProjectGate gate, ToolErrorCode code) {
		assertThatThrownBy(() -> gate.acquire(new CancellationToken()))
				.isInstanceOf(ToolException.class)
				.matches(e -> ((ToolException) e).getCode() == code);
	}
}
