package jadx.gui.device.debugger;

import jadx.gui.device.protocol.ADB;
import jadx.gui.ui.panel.JDebuggerPanel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class LogcatController {
	private ADB.Device adbDevice;
	private JDebuggerPanel debugPanel;
	private ArrayList<info> events = new ArrayList<info>();
	private int recent = 0;

	public LogcatController(JDebuggerPanel debugPanel, ADB.Device adbDevice) throws IOException, InterruptedException {
		this.adbDevice = adbDevice;
		this.debugPanel = debugPanel;
		this.getLog();
	}

	public void getLog() {
		try {
			byte[] buf = adbDevice.getBinaryLogcat();
			ByteBuffer in = ByteBuffer.wrap(buf);
			in.order(ByteOrder.LITTLE_ENDIAN);
			while(in.remaining() > 20) {

				info eInfo = null;
				byte[] msgBuf;
				short eLen = in.getShort();
				short eHdrLen = in.getShort();
				if(eLen + eHdrLen > in.remaining()) {
					return;
				}
				switch(eHdrLen) {
					case 20: //header length 20 == version 1
						eInfo = new info(eLen, eHdrLen, in.getInt(), in.getInt(), in.getInt(), in.getInt(), in.get());
						msgBuf = new byte[eLen];
						in.get(msgBuf,0,eLen-1);
						eInfo.setMsg(msgBuf);
						break;
					case 24: //header length 24 == version 2 / 3
						eInfo = new info(eLen, eHdrLen, in.getInt(), in.getInt(), in.getInt(), in.getInt(), in.getInt(), in.get());
						msgBuf = new byte[eLen];
						in.get(msgBuf,0,eLen-1);
						eInfo.setMsg(msgBuf);
						break;
					case 28: //header length 28 == version 4
						eInfo = new info(eLen, eHdrLen, in.getInt(), in.getInt(), in.getInt(), in.getInt(), in.getInt(), in.getInt(), in.get());
						msgBuf = new byte[eLen];
						in.get(msgBuf,0,eLen-1);
						eInfo.setMsg(msgBuf);
						break;
					default:
						debugPanel.logcatUpdate("Unknown Logcat Version");
						break;
				}
				if(eInfo == null) {
					return;
				}
				if(recent < eInfo.getNSec()) {
					recent = eInfo.getNSec();
				}
				events.add(eInfo);
			}

		} catch (Exception except) {
			debugPanel.log("Logcat Failed");
			except.printStackTrace();
		}
	}

	private class info {
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
		public info(short len, short hdr_size, int pid, int tid, int sec, int nsec, byte msgType) {
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
		public info(short len, short hdr_size, int pid, int tid, int sec, int nsec, int lid, byte msgType) {
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

		public info(short len, short hdr_size, int pid, int tid, int sec, int nsec, int lid, int uid, byte msgType) {
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

		public byte getMsgType() {
			return this.msgType;
		}

		public String getMsg() {
			return this.msg;
		}


	}

}
