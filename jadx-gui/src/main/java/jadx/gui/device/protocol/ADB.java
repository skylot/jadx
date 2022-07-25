package jadx.gui.device.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.log.LogUtils;
import jadx.gui.utils.IOUtils;

public class ADB {
	private static final Logger LOG = LoggerFactory.getLogger(ADB.class);

	private static final int DEFAULT_PORT = 5037;
	private static final String DEFAULT_ADDR = "localhost";

	private static final String CMD_FEATURES = "000dhost:features";
	private static final String CMD_TRACK_DEVICES = "0014host:track-devices-l";
	private static final byte[] OKAY = "OKAY".getBytes();
	private static final byte[] FAIL = "FAIL".getBytes();

	static boolean isOkay(InputStream stream) throws IOException {
		byte[] buf = IOUtils.readNBytes(stream, 4);
		if (Arrays.equals(buf, OKAY)) {
			return true;
		}
		if (Arrays.equals(buf, FAIL)) {
			// Observed that after FAIL the length in hex follows and afterwards an error message,
			// but it is unclear if this is true for all cases where isOkay is used.
			// int msgLen = Integer.parseInt(new String(IOUtils.readNBytes(stream, 4)), 16);
			// byte[] errorMsg = IOUtils.readNBytes(stream, msgLen);
			// LOG.error("isOkay failed: received error message: {}", new String(errorMsg));
			LOG.error("isOkay failed");
			return false;
		}
		if (buf == null) {
			throw new IOException("isOkay failed - steam ended");
		}
		throw new IOException("isOkay failed - unexpected response " + new String(buf));
	}

	public static byte[] exec(String cmd, OutputStream outputStream, InputStream inputStream) throws IOException {
		return execCommandSync(outputStream, inputStream, cmd);
	}

	public static byte[] exec(String cmd) throws IOException {
		try (Socket socket = connect()) {
			return exec(cmd, socket.getOutputStream(), socket.getInputStream());
		}
	}

	public static Socket connect() throws IOException {
		return connect(DEFAULT_ADDR, DEFAULT_PORT);
	}

	public static Socket connect(String host, int port) throws IOException {
		return new Socket(host, port);
	}

	static boolean execCommandAsync(OutputStream outputStream, InputStream inputStream, String cmd) throws IOException {
		outputStream.write(cmd.getBytes());
		return isOkay(inputStream);
	}

	private static byte[] execCommandSync(OutputStream outputStream, InputStream inputStream, String cmd) throws IOException {
		outputStream.write(cmd.getBytes());
		if (isOkay(inputStream)) {
			return readServiceProtocol(inputStream);
		}
		return null;
	}

	static byte[] readServiceProtocol(InputStream stream) {
		try {
			byte[] buf = IOUtils.readNBytes(stream, 4);
			if (buf == null) {
				return null;
			}
			int len = unhex(buf);
			byte[] result;
			if (len == 0) {
				result = new byte[0];
			} else {
				result = IOUtils.readNBytes(stream, len);
			}
			if (LOG.isTraceEnabled()) {
				LOG.trace("readServiceProtocol result: {}", LogUtils.escape(result));
			}
			return result;
		} catch (SocketException e) {
			LOG.warn("Aborting readServiceProtocol: {}", e.toString());
		} catch (IOException e) {
			LOG.error("Failed to read readServiceProtocol", e);
		}
		return null;
	}

	static boolean setSerial(String serial, OutputStream outputStream, InputStream inputStream) throws IOException {
		String setSerialCmd = String.format("host:tport:serial:%s", serial);
		setSerialCmd = String.format("%04x%s", setSerialCmd.length(), setSerialCmd);
		outputStream.write(setSerialCmd.getBytes());
		boolean ok = isOkay(inputStream);
		if (ok) {
			// skip the shell-state-id returned by ADB server, it's not important for the following actions.
			IOUtils.readNBytes(inputStream, 8);
		} else {
			LOG.error("setSerial command {} failed", LogUtils.escape(setSerialCmd));
		}
		return ok;
	}

	private static byte[] execShellCommandRaw(String cmd, OutputStream outputStream, InputStream inputStream) throws IOException {
		cmd = String.format("shell,v2,TERM=xterm-256color,raw:%s", cmd);
		cmd = String.format("%04x%s", cmd.length(), cmd);
		outputStream.write(cmd.getBytes());
		if (isOkay(inputStream)) {
			return ShellProtocol.readStdout(inputStream);
		}
		return null;
	}

	static byte[] execShellCommandRaw(String serial, String cmd, OutputStream outputStream, InputStream inputStream) throws IOException {
		if (setSerial(serial, outputStream, inputStream)) {
			return execShellCommandRaw(cmd, outputStream, inputStream);
		}
		return null;
	}

	public static List<String> getFeatures() throws IOException {
		byte[] rst = exec(CMD_FEATURES);
		if (rst != null) {
			return Arrays.asList(new String(rst).trim().split(","));
		}
		return Collections.emptyList();
	}

	public static boolean startServer(String adbPath, int port) throws IOException {
		String tcpPort = String.format("tcp:%d", port);
		List<String> command = Arrays.asList(adbPath, "-L", tcpPort, "start-server");
		java.lang.Process proc = new ProcessBuilder(command)
				.redirectErrorStream(true)
				.start();
		try {
			// Wait for the adb server to start. On Windows even on a fast system 6 seconds are not unusual.
			proc.waitFor(10, TimeUnit.SECONDS);
			proc.exitValue();
		} catch (Exception e) {
			LOG.error("ADB start server failed with command: {}", String.join(" ", command), e);
			proc.destroyForcibly();
			return false;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (InputStream in = proc.getInputStream()) {
			int read;
			byte[] buf = new byte[1024];
			while ((read = in.read(buf)) >= 0) {
				out.write(buf, 0, read);
			}
		}
		return out.toString().contains(tcpPort);
	}

	public static boolean isServerRunning(String host, int port) {
		try (Socket sock = new Socket(host, port)) {
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * @return a socket connected to adb server, otherwise null
	 */
	public static Socket listenForDeviceState(DeviceStateListener listener, String host, int port) throws IOException {
		Socket socket = connect(host, port);
		InputStream inputStream = socket.getInputStream();
		OutputStream outputStream = socket.getOutputStream();
		if (!execCommandAsync(outputStream, inputStream, CMD_TRACK_DEVICES)) {
			socket.close();
			return null;
		}
		ExecutorService listenThread = Executors.newFixedThreadPool(1);
		listenThread.execute(() -> {
			while (true) {
				byte[] res = readServiceProtocol(inputStream);
				if (res == null) {
					break; // socket disconnected
				}
				if (listener != null) {
					String payload = new String(res);
					String[] deviceLines = payload.split("\n");
					List<ADBDeviceInfo> deviceInfoList = new ArrayList<>(deviceLines.length);
					for (String deviceLine : deviceLines) {
						if (!deviceLine.trim().isEmpty()) {
							deviceInfoList.add(new ADBDeviceInfo(deviceLine, host, port));
						}
					}
					listener.onDeviceStatusChange(deviceInfoList);
				}
			}
			if (listener != null) {
				listener.adbDisconnected();
			}
		});
		return socket;
	}

	public static List<String> listForward(String host, int port) throws IOException {
		try (Socket socket = connect(host, port)) {
			String cmd = "0011host:list-forward";
			InputStream inputStream = socket.getInputStream();
			OutputStream outputStream = socket.getOutputStream();
			outputStream.write(cmd.getBytes());
			if (isOkay(inputStream)) {
				byte[] bytes = readServiceProtocol(inputStream);
				if (bytes != null) {
					String[] forwards = new String(bytes).split("\n");
					return Stream.of(forwards).map(String::trim).collect(Collectors.toList());
				}
			}
		}
		return Collections.emptyList();
	}

	public static boolean removeForward(String host, int port, String serial, String localPort) throws IOException {
		try (Socket socket = connect(host, port)) {
			String cmd = String.format("host:killforward:tcp:%s", localPort);
			cmd = String.format("%04x%s", cmd.length(), cmd);
			InputStream inputStream = socket.getInputStream();
			OutputStream outputStream = socket.getOutputStream();
			if (setSerial(serial, outputStream, inputStream)) {
				outputStream.write(cmd.getBytes());
				return isOkay(inputStream) && isOkay(inputStream);
			}
		}
		return false;
	}

	// Little endian
	private static int readInt(byte[] bytes, int start) {
		int result = (bytes[start] & 0xff);
		result += ((bytes[start + 1] & 0xff) << 8);
		result += ((bytes[start + 2] & 0xff) << 16);
		result += (bytes[start + 3] & 0xff) << 24;
		return result;
	}

	private static byte[] appendBytes(byte[] dest, byte[] src, int realSrcSize) {
		byte[] rst = new byte[dest.length + realSrcSize];
		System.arraycopy(dest, 0, rst, 0, dest.length);
		System.arraycopy(src, 0, rst, dest.length, realSrcSize);
		return rst;
	}

	private static int unhex(byte[] hex) {
		int n = 0;
		byte b;
		for (int i = 0; i < 4; i++) {
			b = hex[i];
			switch (b) {
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
					b -= '0';
					break;
				case 'a':
				case 'b':
				case 'c':
				case 'd':
				case 'e':
				case 'f':
					b = (byte) (b - 'a' + 10);
					break;
				case 'A':
				case 'B':
				case 'C':
				case 'D':
				case 'E':
				case 'F':
					b = (byte) (b - 'A' + 10);
					break;
				default:
					return -1;
			}
			n = (n << 4) | (b & 0xff);
		}
		return n;
	}

	public interface JDWPProcessListener {
		void jdwpProcessOccurred(ADBDevice device, Set<String> id);

		void jdwpListenerClosed(ADBDevice device);
	}

	public interface DeviceStateListener {
		void onDeviceStatusChange(List<ADBDeviceInfo> deviceInfoList);

		void adbDisconnected();
	}

	public static class Process {
		public String user;
		public String pid;
		public String ppid;
		public String name;

		public static Process make(String processLine) {
			String[] fields = processLine.split("\\s+");
			if (fields.length >= 4) {
				// 0 for user, 1 for pid, 2 for ppid, the last one for name
				Process proc = new Process();
				proc.user = fields[0];
				proc.pid = fields[1];
				proc.ppid = fields[2];
				proc.name = fields[fields.length - 1];
				return proc;
			}
			return null;
		}
	}

	private static class ShellProtocol {
		private static final int ID_STD_IN = 0;
		private static final int ID_STD_OUT = 1;
		private static final int ID_STD_ERR = 2;
		private static final int ID_EXIT = 3;

		// Close subprocess stdin if possible.
		private static final int ID_CLOSE_STDIN = 4;

		// Window size change (an ASCII version of struct winsize).
		private static final int ID_WINDOW_SIZE_CHANGE = 5;

		// Indicates an invalid or unknown packet.
		private static final int ID_INVALID = 255;

		public static byte[] readStdout(InputStream inputStream) throws IOException {
			byte[] header = new byte[5];
			ByteArrayOutputStream payload = new ByteArrayOutputStream();
			byte[] tempBuf = new byte[1024];
			for (boolean exit = false; !exit;) {
				IOUtils.read(inputStream, header);
				exit = header[0] == ID_EXIT;
				int payloadSize = readInt(header, 1);
				if (tempBuf.length < payloadSize) {
					tempBuf = new byte[payloadSize];
				}
				int readSize = IOUtils.read(inputStream, tempBuf, 0, payloadSize);
				if (readSize != payloadSize) {
					LOG.error("Failed to read ShellProtocol data");
					return null; // we don't want corrupted data.
				}
				payload.write(tempBuf, 0, readSize);
			}
			return payload.toByteArray();
		}
	}
}
