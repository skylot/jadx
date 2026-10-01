package jadx.gui.links;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class JadxLinkServerTest {

	@TempDir
	Path tempDir;

	@Test
	void forwardLink() throws Exception {
		Path infoFile = tempDir.resolve("link-server.txt");
		LinkedBlockingQueue<String> received = new LinkedBlockingQueue<>();
		new JadxLinkServer(infoFile, received::add).start();

		String link = "jadx://3fa9c01b7e/a.B.m()";
		assertThat(JadxLinkServer.sendToRunningInstance(infoFile, link)).isTrue();
		assertThat(received.poll(5, TimeUnit.SECONDS)).isEqualTo(link);

		// only jadx links accepted
		assertThat(JadxLinkServer.sendToRunningInstance(infoFile, "/etc/passwd")).isFalse();

		// wrong token rejected
		List<String> lines = Files.readAllLines(infoFile, StandardCharsets.UTF_8);
		Path badInfo = tempDir.resolve("bad.txt");
		Files.writeString(badInfo, lines.get(0) + "\nbadtoken\n");
		assertThat(JadxLinkServer.sendToRunningInstance(badInfo, link)).isFalse();
		assertThat(received).isEmpty();

		// missing info file => no instance
		assertThat(JadxLinkServer.sendToRunningInstance(tempDir.resolve("none.txt"), link)).isFalse();
	}
}
