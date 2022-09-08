package jadx.gui.device.debugger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.device.protocol.ADBDevice;
import jadx.gui.ui.panel.LogcatPanel;

public class LogcatController {
	private static final Logger LOG = LoggerFactory.getLogger(LogcatController.class);

	private final ADBDevice adbDevice;
	private final LogcatPanel logcatPanel;
	private Timer timer;
	private final String timezone;
	private LogcatInfo recent = null;
	private ArrayList<LogcatInfo> events = new ArrayList<>();
	private LogcatFilter filter = new LogcatFilter(null, null);
	private String status = "null";

	public LogcatController(LogcatPanel logcatPanel, ADBDevice adbDevice) throws IOException {
		this.adbDevice = adbDevice;
		this.logcatPanel = logcatPanel;
		this.timezone = adbDevice.getTimezone();
		this.startLogcat();
	}

	public void startLogcat() {
		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				getLog();
			}
		}, 0, 1000);
		this.status = "running";
	}

	public void stopLogcat() {
		timer.cancel();
		this.status = "stopped";
	}

	public String getStatus() {
		return this.status;
	}

	public void clearLogcat() {
		try {
			adbDevice.clearLogcat();
			clearEvents();
		} catch (IOException e) {
			LOG.error("Failed to clear Logcat", e);
		}
	}

	private void getLog() {
		if (!logcatPanel.isReady()) {
			return;
		}
		try {
			byte[] buf;
			if (recent == null) {
				buf = adbDevice.getBinaryLogcat();
			} else {
				buf = adbDevice.getBinaryLogcat(recent.getAfterTimestamp());
			}
			if (buf == null) {
				return;
			}
			ByteBuffer in = ByteBuffer.wrap(buf);
			in.order(ByteOrder.LITTLE_ENDIAN);
			while (in.remaining() > 20) {

				LogcatInfo eInfo = null;
				byte[] msgBuf;
				short eLen = in.getShort();
				short eHdrLen = in.getShort();
				if (eLen + eHdrLen > in.remaining()) {
					return;
				}
				switch (eHdrLen) {
					case 20: // header length 20 == version 1
						eInfo = new LogcatInfo(eLen, eHdrLen, in.getInt(), in.getInt(), in.getInt(), in.getInt(), in.get());
						msgBuf = new byte[eLen];
						in.get(msgBuf, 0, eLen - 1);
						eInfo.setMsg(msgBuf);
						break;
					case 24: // header length 24 == version 2 / 3
						eInfo = new LogcatInfo(eLen, eHdrLen, in.getInt(), in.getInt(), in.getInt(), in.getInt(), in.getInt(), in.get());
						msgBuf = new byte[eLen];
						in.get(msgBuf, 0, eLen - 1);
						eInfo.setMsg(msgBuf);
						break;
					case 28: // header length 28 == version 4
						eInfo = new LogcatInfo(eLen, eHdrLen, in.getInt(), in.getInt(), in.getInt(), in.getInt(), in.getInt(), in.getInt(),
								in.get());
						msgBuf = new byte[eLen];
						in.get(msgBuf, 0, eLen - 1);
						eInfo.setMsg(msgBuf);
						break;
					default:

						break;
				}
				if (eInfo == null) {
					return;
				}
				if (recent == null) {
					recent = eInfo;
				} else if (recent.getInstant().isBefore(eInfo.getInstant())) {
					recent = eInfo;
				}

				if (filter.doFilter(eInfo)) {
					logcatPanel.log(eInfo);
				}
				events.add(eInfo);
			}

		} catch (Exception e) {
			LOG.error("Failed to get logcat message", e);
		}
	}

	public boolean reload() {
		stopLogcat();
		boolean ok = logcatPanel.clearLogcatArea();
		if (ok) {
			events.forEach((eInfo) -> {
				if (filter.doFilter(eInfo)) {
					logcatPanel.log(eInfo);
				}
			});
			startLogcat();
		}
		return true;
	}

	public void clearEvents() {
		this.recent = null;
		this.events = new ArrayList<>();
	}

	public void exit() {
		stopLogcat();
		filter = new LogcatFilter(null, null);
		recent = null;
	}

	public LogcatFilter getFilter() {
		return this.filter;
	}

	public class LogcatFilter {
		private final ArrayList<Integer> pid;
		private ArrayList<Byte> msgType = new ArrayList<Byte>() {
			{
				add((byte) 1);
				add((byte) 2);
				add((byte) 3);
				add((byte) 4);
				add((byte) 5);
				add((byte) 6);
				add((byte) 7);
				add((byte) 8);
			}
		};

		public LogcatFilter(ArrayList<Integer> pid, ArrayList<Byte> msgType) {
			if (pid != null) {
				this.pid = pid;
			} else {
				this.pid = new ArrayList<>();
			}

			if (msgType != null) {
				this.msgType = msgType;
			}
		}

		public void addPid(int pid) {

			if (!this.pid.contains(pid)) {
				this.pid.add(pid);
			}
		}

		public void removePid(int pid) {
			int pidPos = this.pid.indexOf(pid);
			if (pidPos >= 0) {
				this.pid.remove(pidPos);
			}
		}

		public void togglePid(int pid, boolean state) {
			if (state) {
				addPid(pid);
			} else {
				removePid(pid);
			}
		}

		public void addMsgType(byte msgType) {
			if (!this.msgType.contains(msgType)) {
				this.msgType.add(msgType);
			}
		}

		public void removeMsgType(byte msgType) {
			int typePos = this.msgType.indexOf(msgType);
			if (typePos >= 0) {
				this.msgType.remove(typePos);
			}
		}

		public void toggleMsgType(byte msgType, boolean state) {
			if (state) {
				addMsgType(msgType);
			} else {
				removeMsgType(msgType);
			}
		}

		public boolean doFilter(LogcatInfo inInfo) {
			if (pid.contains(inInfo.getPid())) {
				return msgType.contains(inInfo.getMsgType());
			}
			return false;
		}

		public ArrayList<LogcatInfo> getFilteredList(ArrayList<LogcatInfo> inInfoList) {
			ArrayList<LogcatInfo> outInfoList = new ArrayList<LogcatInfo>();
			inInfoList.forEach((inInfo) -> {
				if (doFilter(inInfo)) {
					outInfoList.add(inInfo);
				}
			});
			return outInfoList;
		}
	}

	public class LogcatInfo {
		private String msg;
		private final byte msgType;
		private final int nsec;
		private final int pid;
		private final int sec;
		private final int tid;
		private final short hdrSize;
		private final short len;
		private final short version;
		private int lid;
		private int uid;

		public LogcatInfo(short len, short hdrSize, int pid, int tid, int sec, int nsec, byte msgType) {
			this.hdrSize = hdrSize;
			this.len = len;
			this.msgType = msgType;
			this.nsec = nsec;
			this.pid = pid;
			this.sec = sec;
			this.tid = tid;
			this.version = 1;
		}

		// Version 2 and 3 both have the same arguments
		public LogcatInfo(short len, short hdrSize, int pid, int tid, int sec, int nsec, int lid, byte msgType) {
			this.hdrSize = hdrSize;
			this.len = len;
			this.lid = lid;
			this.msgType = msgType;
			this.nsec = nsec;
			this.pid = pid;
			this.sec = sec;
			this.tid = tid;
			this.version = 3;
		}

		public LogcatInfo(short len, short hdrSize, int pid, int tid, int sec, int nsec, int lid, int uid, byte msgType) {
			this.hdrSize = hdrSize;
			this.len = len;
			this.lid = lid;
			this.msgType = msgType;
			this.nsec = nsec;
			this.pid = pid;
			this.sec = sec;
			this.tid = tid;
			this.uid = uid;
			this.version = 4;
		}

		public void setMsg(byte[] msg) {
			this.msg = new String(msg);
		}

		public short getVersion() {
			return this.version;
		}

		public short getLen() {
			return this.len;
		}

		public short getHeaderLen() {
			return this.hdrSize;
		}

		public int getPid() {
			return this.pid;
		}

		public int getTid() {
			return this.tid;
		}

		public int getSec() {
			return this.sec;
		}

		public int getNSec() {
			return this.nsec;
		}

		public int getLid() {
			return this.lid;
		}

		public int getUid() {
			return this.uid;
		}

		public Instant getInstant() {
			return Instant.ofEpochSecond(getSec(), getNSec());
		}

		public String getTimestamp() {
			DateTimeFormatter dtFormat = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS").withZone(ZoneId.of(timezone));
			return dtFormat.format(getInstant());
		}

		public String getAfterTimestamp() {
			DateTimeFormatter dtFormat = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS").withZone(ZoneId.of(timezone));
			return dtFormat.format(getInstant().plusMillis(1));
		}

		public byte getMsgType() {
			return this.msgType;
		}

		public String getMsgTypeString() {
			switch (getMsgType()) {
				case 0:
					return "Unknown";
				case 1:
					return "Default";
				case 2:
					return "Verbose";
				case 3:
					return "Debug";
				case 4:
					return "Info";
				case 5:
					return "Warn";
				case 6:
					return "Error";
				case 7:
					return "Fatal";
				case 8:
					return "Silent";
				default:
					return "Unknown";
			}
		}

		public String getMsg() {
			return this.msg;
		}
	}
}
