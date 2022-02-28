package jadx.gui.device.debugger;

import jadx.gui.device.protocol.ADB;
import jadx.gui.ui.panel.JDebuggerPanel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class LogcatController {
	private ADB.Device adbDevice;
	private JDebuggerPanel debugPanel;
	private ArrayList<logcatInfo> events = new ArrayList<logcatInfo>();
	private logcatInfo recent = null;
	private Timer timer;
	private String timezone;

	public LogcatController(JDebuggerPanel debugPanel, ADB.Device adbDevice) throws IOException, InterruptedException {
		this.adbDevice = adbDevice;
		this.debugPanel = debugPanel;
		this.getTimezone();
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
	}

	public void stopLogcat() {
		timer.cancel();
	}

	private void getTimezone() {
		try {
			this.timezone = adbDevice.getTimezone();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	private void getLog() {
		try {
			byte[] buf;
			if(recent == null) {
				buf = adbDevice.getBinaryLogcat();
			} else {
				buf = adbDevice.getBinaryLogcat(recent.getAfterTimestamp());
			}
			if(buf == null) {
				return;
			}
			ByteBuffer in = ByteBuffer.wrap(buf);
			in.order(ByteOrder.LITTLE_ENDIAN);
			while(in.remaining() > 20) {

				logcatInfo eInfo = null;
				byte[] msgBuf;
				short eLen = in.getShort();
				short eHdrLen = in.getShort();
				if(eLen + eHdrLen > in.remaining()) {
					return;
				}
				switch(eHdrLen) {
					case 20: //header length 20 == version 1
						eInfo = new logcatInfo(eLen, eHdrLen, in.getInt(), in.getInt(), in.getInt(), in.getInt(), in.get());
						msgBuf = new byte[eLen];
						in.get(msgBuf,0,eLen-1);
						eInfo.setMsg(msgBuf);
						break;
					case 24: //header length 24 == version 2 / 3
						eInfo = new logcatInfo(eLen, eHdrLen, in.getInt(), in.getInt(), in.getInt(), in.getInt(), in.getInt(), in.get());
						msgBuf = new byte[eLen];
						in.get(msgBuf,0,eLen-1);
						eInfo.setMsg(msgBuf);
						break;
					case 28: //header length 28 == version 4
						eInfo = new logcatInfo(eLen, eHdrLen, in.getInt(), in.getInt(), in.getInt(), in.getInt(), in.getInt(), in.getInt(), in.get());
						msgBuf = new byte[eLen];
						in.get(msgBuf,0,eLen-1);
						eInfo.setMsg(msgBuf);
						break;
					default:
						debugPanel.log("Unknown Logcat Version");
						break;
				}
				if(eInfo == null) {
					return;
				}
				if(recent == null) {
					recent = eInfo;
				} else if(recent.getInstant().isBefore(eInfo.getInstant())) {
					recent = eInfo;
				}
				events.add(eInfo);
				debugPanel.logcatUpdate(eInfo);
			}

		} catch (Exception except) {
			debugPanel.log("Logcat Failed");
			except.printStackTrace();
		}
	}

	public class logcatInfo {
		private short version;
		private short len;
		private short hdr_size;
		private int pid;
		private int tid;
		private int sec;
		private int nsec;
		private int lid;
		private int uid;
		private byte msgType;



		String msg;
		public logcatInfo(short len, short hdr_size, int pid, int tid, int sec, int nsec, byte msgType) {
			this.version = 1;
			this.len = len;
			this.hdr_size = hdr_size;
			this.pid = pid;
			this.tid = tid;
			this.sec = sec;
			this.nsec = nsec;
			this.msgType = msgType;
		}

		//Version 2 and 3 both have the same arguments
		public logcatInfo(short len, short hdr_size, int pid, int tid, int sec, int nsec, int lid, byte msgType) {
			this.version = 3;
			this.len = len;
			this.hdr_size = hdr_size;
			this.pid = pid;
			this.tid = tid;
			this.sec = sec;
			this.nsec = nsec;
			this.lid = lid;
			this.msgType = msgType;
		}

		public logcatInfo(short len, short hdr_size, int pid, int tid, int sec, int nsec, int lid, int uid, byte msgType) {
			this.version = 4;
			this.len = len;
			this.hdr_size = hdr_size;
			this.pid = pid;
			this.tid = tid;
			this.sec = sec;
			this.nsec = nsec;
			this.lid = lid;
			this.uid = uid;
			this.msgType = msgType;
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
			return this.hdr_size;
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
			DateTimeFormatter dtFormat = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS").withZone( ZoneId.of(timezone) );
			return dtFormat.format(getInstant());
		}

		public String getAfterTimestamp() {
			DateTimeFormatter dtFormat = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS").withZone( ZoneId.of(timezone) );
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
