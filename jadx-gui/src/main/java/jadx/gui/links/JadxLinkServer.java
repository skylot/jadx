package jadx.gui.links;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.utils.files.JadxFiles;

/**
 * Single-instance forwarding: a running jadx-gui listens on a random loopback port and a
 * new process started with a jadx:// link hands it over instead of opening a second window.
 * The port and a random token are written to a config file; the token must match to be accepted.
 */
public class JadxLinkServer {
	private static final Logger LOG = LoggerFactory.getLogger(JadxLinkServer.class);
	private static final int TIMEOUT_MS = 3000;

	private final Path infoFile;
	private final Consumer<String> linkHandler;
	private final String token = randomToken();

	public JadxLinkServer(Consumer<String> linkHandler) {
		this(JadxFiles.LINK_SERVER, linkHandler);
	}

	public JadxLinkServer(Path infoFile, Consumer<String> linkHandler) {
		this.infoFile = infoFile;
		this.linkHandler = linkHandler;
	}

	/** @return true if a running instance accepted the link */
	public static boolean sendToRunningInstance(String link) {
		return sendToRunningInstance(JadxFiles.LINK_SERVER, link);
	}

	public static boolean sendToRunningInstance(Path infoFile, String link) {
		try {
			List<String> lines = Files.readAllLines(infoFile, StandardCharsets.UTF_8);
			try (Socket socket = new Socket()) {
				socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), Integer.parseInt(lines.get(0).trim())), TIMEOUT_MS);
				socket.setSoTimeout(TIMEOUT_MS);
				socket.getOutputStream().write((lines.get(1).trim() + '\n' + link.trim() + '\n').getBytes(StandardCharsets.UTF_8));
				socket.getOutputStream().flush();
				return "OK".equals(new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)).readLine());
			}
		} catch (Exception e) {
			return false; // no reachable instance
		}
	}

	public void start() {
		try {
			ServerSocket server = new ServerSocket(0, 10, InetAddress.getLoopbackAddress());
			writeInfoFile(server.getLocalPort());
			Thread t = new Thread(() -> acceptLoop(server), "jadx-link-server");
			t.setDaemon(true);
			t.start();
		} catch (Exception e) {
			LOG.warn("Failed to start jadx link server, links will open in new windows", e);
		}
	}

	private void acceptLoop(ServerSocket server) {
		while (!server.isClosed()) {
			try (Socket client = server.accept()) {
				client.setSoTimeout(TIMEOUT_MS);
				BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
				String clientToken = in.readLine();
				String link = in.readLine();
				boolean ok = clientToken != null && tokenMatches(clientToken) && JadxLink.isJadxLink(link);
				OutputStream out = client.getOutputStream();
				out.write((ok ? "OK\n" : "ERR\n").getBytes(StandardCharsets.UTF_8));
				out.flush();
				if (ok) {
					linkHandler.accept(link);
				}
			} catch (Exception e) {
				LOG.debug("jadx link server connection error", e);
			}
		}
	}

	private boolean tokenMatches(String given) {
		return MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8));
	}

	private void writeInfoFile(int port) throws Exception {
		Files.createDirectories(infoFile.getParent());
		Files.writeString(infoFile, port + "\n" + token + "\n", StandardCharsets.UTF_8);
		try {
			Files.setPosixFilePermissions(infoFile, PosixFilePermissions.fromString("rw-------"));
		} catch (Exception ignore) {
			// non-POSIX filesystem: config dir is already user-private
		}
	}

	private static String randomToken() {
		byte[] bytes = new byte[24];
		new SecureRandom().nextBytes(bytes);
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}
