package jadx.gui.device.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.reactivex.annotations.NonNull;

import jadx.core.utils.StringUtils;

public class ADB {
	private static final int DEFAULT_PORT = 5037;
	private static final String DEFAULT_ADDR = "localhost";

	private static final String CMD_FEATURES = "000dhost:features";
	private static final String CMD_TRACK_JDWP = "000atrack-jdwp";
	private static final String CMD_TRACK_DEVICES = "0014host:track-devices-l";
	private static final byte[] OKAY = "OKAY".getBytes();

	private static boolean isOkay(InputStream stream) throws IOException {
		byte[] buf = new byte[4];
		stream.read(buf, 0, 4);
		return Arrays.equals(buf, OKAY);
	}

	public static byte[] exec(String cmd, OutputStream outputStream, InputStream inputStream) throws IOException {
		return execCommandSync(outputStream, inputStream, cmd);
	}

	public static byte[] exec(String cmd) throws IOException {
		byte[] res;
		Socket socket = connect();
		res = exec(cmd, socket.getOutputStream(), socket.getInputStream());
		socket.close();
		return res;
	}

	public static Socket connect() throws IOException {
		return connect(DEFAULT_ADDR, DEFAULT_PORT);
	}

	public static Socket connect(String host, int port) throws IOException {
		return new Socket(host, port);
	}

	private static boolean execCommandAsync(OutputStream outputStream,
			InputStream inputStream, String cmd) throws IOException {
		outputStream.write(cmd.getBytes());
		return isOkay(inputStream);
	}

	private static byte[] execCommandSync(OutputStream outputStream,
			InputStream inputStream, String cmd) throws IOException {
		outputStream.write(cmd.getBytes());
		if (isOkay(inputStream)) {
			return readServiceProtocol(inputStream);
		}
		return null;
	}

	private static byte[] readServiceProtocol(InputStream stream) {
		byte[] bytes = null;
		byte[] buf = new byte[4];
		try {
			int len = stream.read(buf, 0, 4);
			if (len == 4) {
				len = unhex(buf);
				if (len == 0) {
					return new byte[0];
				}
				if (len != -1) {
					buf = new byte[len];
					if (stream.read(buf, 0, len) == len) {
						bytes = buf;
					}
				}
			}
		} catch (IOException ignore) {
		}
		return bytes;
	}

	private static boolean setSerial(String serial, OutputStream outputStream, InputStream inputStream) throws IOException {
		String setSerialCmd = String.format("host:tport:serial:%s", serial);
		setSerialCmd = String.format("%04x%s", setSerialCmd.length(), setSerialCmd);
		outputStream.write(setSerialCmd.getBytes());
		boolean ok = isOkay(inputStream);
		if (ok) {
			// skip the shell-state-id returned by ADB server, it's not important for the following actions.
			ok = inputStream.skip(8) == 8;
		}
		return ok;
	}

	private static byte[] execShellCommandRaw(String cmd,
			OutputStream outputStream, InputStream inputStream) throws IOException {

		cmd = String.format("shell,v2,TERM=xterm-256color,raw:%s", cmd);
		cmd = String.format("%04x%s", cmd.length(), cmd);
		outputStream.write(cmd.getBytes());
		if (isOkay(inputStream)) {
			return ShellProtocol.readStdout(inputStream);
		}
		return null;
	}

	private static byte[] execShellCommandRaw(String serial, String cmd,
			OutputStream outputStream, InputStream inputStream) throws IOException {
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
		java.lang.Process proc = new ProcessBuilder(adbPath, "-L", tcpPort, "start-server")
				.redirectErrorStream(true)
				.start();
		try {
			proc.waitFor(3, TimeUnit.SECONDS); // for listening to a port, 3 sec should be more than enough.
			proc.exitValue();
		} catch (InterruptedException e) {
			e.printStackTrace();
			proc.destroyForcibly();
			return false;
		}
		InputStream is = proc.getInputStream();
		int size = is.available();
		byte[] bytes = new byte[size];
		is.read(bytes, 0, size);
		return new String(bytes).contains(tcpPort);
	}

	public static boolean isServerRunning(String host, int port) {
		try {
			Socket sock = new Socket(host, port);
			sock.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
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
			for (;;) {
				byte[] res = readServiceProtocol(inputStream);
				if (res != null) {
					if (listener != null) {
						String payload = new String(res);
						String[] deviceLines = payload.split("\n");
						List<DeviceInfo> deviceInfoList = new ArrayList<>(deviceLines.length);
						for (String deviceLine : deviceLines) {
							if (!deviceLine.trim().isEmpty()) {
								deviceInfoList.add(DeviceInfo.make(deviceLine, host, port));
							}
						}
						listener.onDeviceStatusChange(deviceInfoList);
					}
				} else { // socket disconnected
					break;
				}
			}
			if (listener != null) {
				listener.adbDisconnected();
			}
		});
		return socket;
	}

	public static List<String> listForward(String host, int port) throws IOException {
		Socket socket = connect(host, port);
		String cmd = "0011host:list-forward";
		InputStream inputStream = socket.getInputStream();
		OutputStream outputStream = socket.getOutputStream();
		outputStream.write(cmd.getBytes());
		if (isOkay(inputStream)) {
			byte[] bytes = readServiceProtocol(inputStream);
			if (bytes != null) {
				String[] forwards = new String(bytes).split("\n");
				List<String> forwardList = new ArrayList<>(forwards.length);
				for (String forward : forwards) {
					forwardList.add(forward.trim());
				}
				socket.close();
				return forwardList;
			}
		}
		socket.close();
		return Collections.emptyList();
	}

	public static boolean removeForward(String host, int port, String serial, String localPort) throws IOException {
		Socket socket = connect(host, port);
		String cmd = String.format("host:killforward:tcp:%s", localPort);
		cmd = String.format("%04x%s", cmd.length(), cmd);
		InputStream inputStream = socket.getInputStream();
		OutputStream outputStream = socket.getOutputStream();
		boolean ok = false;
		if (setSerial(serial, outputStream, inputStream)) {
			outputStream.write(cmd.getBytes());
			ok = isOkay(inputStream) && isOkay(inputStream);
		}
		socket.close();
		return ok;
	}

	// Little endian
	private static int readInt(byte[] bytes, int start) {
		int result = 0;
		result = (bytes[start] & 0xff);
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
		void jdwpProcessOccurred(Device device, Set<String> id);

		void jdwpListenerClosed(Device device);
	}

	public interface DeviceStateListener {
		void onDeviceStatusChange(List<DeviceInfo> deviceInfoList);

		void adbDisconnected();
	}

	public static class Device {
		DeviceInfo info;
		String androidReleaseVer;
		volatile Socket jdwpListenerSock;

		public Device(DeviceInfo info) {
			this.info = info;
		}

		public DeviceInfo getDeviceInfo() {
			return info;
		}

		public boolean updateDeviceInfo(DeviceInfo info) {
			boolean matched = this.info.serial.equals(info.serial);
			if (matched) {
				this.info = info;
			}
			return matched;
		}

		public String getSerial() {
			return info.serial;
		}

		public boolean removeForward(String localPort) throws IOException {
			return ADB.removeForward(info.adbHost, info.adbPort, info.serial, localPort);
		}

		public ForwardResult forwardJDWP(String localPort, String jdwpPid) throws IOException {
			Socket socket = connect(info.adbHost, info.adbPort);
			String cmd = String.format("host:forward:tcp:%s;jdwp:%s", localPort, jdwpPid);
			cmd = String.format("%04x%s", cmd.length(), cmd);
			InputStream inputStream = socket.getInputStream();
			OutputStream outputStream = socket.getOutputStream();
			ForwardResult rst;
			if (setSerial(info.serial, outputStream, inputStream)) {
				outputStream.write(cmd.getBytes());
				if (!isOkay(inputStream)) {
					rst = new ForwardResult(1, readServiceProtocol(inputStream));
				} else if (!isOkay(inputStream)) {
					rst = new ForwardResult(2, readServiceProtocol(inputStream));
				} else {
					rst = new ForwardResult(0, null);
				}
			} else {
				rst = new ForwardResult(1, "Unknown error.".getBytes());
			}
			socket.close();
			return rst;
		}

		public static class ForwardResult {
			/**
			 * 0 for success, 1 for failed at binding to local tcp, 2 for failed at remote.
			 */
			public int state;
			public String desc;

			public ForwardResult(int state, byte[] desc) {
				if (desc != null) {
					this.desc = new String(desc);
				} else {
					this.desc = "";
				}
				this.state = state;
			}
		}

		/**
		 * @return pid otherwise -1
		 */
		public int launchApp(String fullAppName) throws IOException, InterruptedException {
			Socket socket = connect(info.adbHost, info.adbPort);
			String cmd = "am start -D -n " + fullAppName;
			byte[] res = execShellCommandRaw(info.serial, cmd, socket.getOutputStream(), socket.getInputStream());
			socket.close();
			String rst = new String(res).trim();
			if (rst.startsWith("Starting: Intent {") && rst.endsWith(fullAppName + " }")) {
				Thread.sleep(40);
				String pkg = fullAppName.split("/")[0];
				for (Process process : getProcessByPkg(pkg)) {
					return Integer.parseInt(process.pid);
				}
			}
			return -1;
		}

		public String getAndroidReleaseVersion() {
			if (!StringUtils.isEmpty(androidReleaseVer)) {
				return androidReleaseVer;
			}
			try {
				List<String> list = getProp("ro.build.version.release");
				if (list.size() != 0) {
					androidReleaseVer = list.get(0);
				}
			} catch (IOException e) {
				e.printStackTrace();
				androidReleaseVer = "";
			}
			return androidReleaseVer;
		}

		public List<String> getProp(String entry) throws IOException {
			Socket socket = connect(info.adbHost, info.adbPort);
			List<String> props = Collections.emptyList();
			String cmd = "getprop";
			if (!StringUtils.isEmpty(entry)) {
				cmd += " " + entry;
			}
			byte[] payload = execShellCommandRaw(info.serial, cmd,
					socket.getOutputStream(), socket.getInputStream());
			if (payload != null) {
				props = new ArrayList<>();
				String[] lines = new String(payload).split("\n");
				for (String line : lines) {
					line = line.trim();
					if (!line.isEmpty()) {
						props.add(line.trim());
					}
				}
			}
			socket.close();
			return props;
		}

		public List<Process> getProcessByPkg(String pkg) throws IOException {
			return getProcessList("ps | grep " + pkg, 0);
		}

		@NonNull
		public List<Process> getProcessList() throws IOException {
			return getProcessList("ps", 1);
		}

		private List<Process> getProcessList(String cmd, int index) throws IOException {
			Socket socket = connect(info.adbHost, info.adbPort);
			List<Process> procs = Collections.emptyList();
			byte[] payload = execShellCommandRaw(info.serial, cmd,
					socket.getOutputStream(), socket.getInputStream());
			if (payload != null) {
				String ps = new String(payload);
				String[] psLines = ps.split("\n");
				for (int i = index; i < psLines.length; i++) {
					Process proc = Process.make(psLines[i]);
					if (proc != null) {
						if (procs.isEmpty()) {
							procs = new ArrayList<>();
						}
						procs.add(proc);
					}
				}
			}
			socket.close();
			return procs;
		}

		public boolean listenForJDWP(JDWPProcessListener listener) throws IOException {
			if (this.jdwpListenerSock != null) {
				return false;
			}
			jdwpListenerSock = connect(this.info.adbHost, this.info.adbPort);
			InputStream inputStream = jdwpListenerSock.getInputStream();
			OutputStream outputStream = jdwpListenerSock.getOutputStream();
			if (setSerial(info.serial, outputStream, inputStream)
					&& execCommandAsync(outputStream, inputStream, CMD_TRACK_JDWP)) {
				Executors.newFixedThreadPool(1).execute(() -> {
					for (;;) {
						byte[] res = readServiceProtocol(inputStream);
						if (res != null) {
							if (listener != null) {
								String payload = new String(res);
								String[] ids = payload.split("\n");
								Set<String> idList = new HashSet<>(ids.length);
								for (String id : ids) {
									if (!id.trim().isEmpty()) {
										idList.add(id);
									}
								}
								listener.jdwpProcessOccurred(this, idList);
							}
						} else { // socket disconnected
							break;
						}
					}
					if (listener != null) {
						this.jdwpListenerSock = null;
						listener.jdwpListenerClosed(this);
					}
				});
			} else {
				jdwpListenerSock.close();
				jdwpListenerSock = null;
				return false;
			}
			return true;
		}

		public void stopListenForJDWP() {
			if (jdwpListenerSock != null) {
				try {
					jdwpListenerSock.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			this.jdwpListenerSock = null;
		}

		@Override
		public int hashCode() {
			return info.serial.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Device) {
				return ((Device) obj).getDeviceInfo().serial.equals(info.serial);
			}
			return false;
		}

		@Override
		public String toString() {
			return info.allInfo;
		}
	}

	public static class DeviceInfo {
		public String adbHost;
		public int adbPort;
		public String serial;
		public String state;
		public String model;
		public String allInfo;

		public boolean isOnline() {
			return state.equals("device");
		}

		@Override
		public String toString() {
			return allInfo;
		}

		static DeviceInfo make(String info, String host, int port) {
			DeviceInfo deviceInfo = new DeviceInfo();
			String[] infoFields = info.trim().split("\\s+");
			deviceInfo.allInfo = String.join(" ", infoFields);
			if (infoFields.length > 2) {
				deviceInfo.serial = infoFields[0];
				deviceInfo.state = infoFields[1];
			}
			int pos = info.indexOf("model:");
			if (pos != -1) {
				int spacePos = info.indexOf(" ", pos);
				if (spacePos != -1) {
					deviceInfo.model = info.substring(pos + "model:".length(), spacePos);
				}
			}
			if (deviceInfo.model == null || deviceInfo.model.equals("")) {
				deviceInfo.model = deviceInfo.serial;
			}
			deviceInfo.adbHost = host;
			deviceInfo.adbPort = port;
			return deviceInfo;
		}
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
			byte[] payload = new byte[0];
			byte[] tempBuf = new byte[0];
			for (boolean exit = false; !exit;) {
				if (inputStream.read(header, 0, 5) == 5) {
					exit = header[0] == ID_EXIT;
					int payloadSize = readInt(header, 1);
					if (tempBuf.length < payloadSize) {
						tempBuf = new byte[payloadSize];
					}
					int readSize = inputStream.read(tempBuf, 0, payloadSize);
					if (readSize != payloadSize) {
						return null; // we don't want corrupted data.
					}
					payload = appendBytes(payload, tempBuf, readSize);
				}
			}
			return payload;
		}
	}
}
