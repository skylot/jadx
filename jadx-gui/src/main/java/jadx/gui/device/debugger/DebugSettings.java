package jadx.gui.device.debugger;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.StringUtils;
import jadx.gui.device.protocol.ADB;
import jadx.gui.device.protocol.ADBDevice;
import jadx.gui.utils.NLS;

public class DebugSettings {
	private static final Logger LOG = LoggerFactory.getLogger(DebugSettings.class);

	private static final int FORWARD_TCP_PORT = 33233;

	public static final DebugSettings INSTANCE = new DebugSettings();

	private int ver;
	private String pid;
	private String name;
	private ADBDevice device;
	private int forwardTcpPort = FORWARD_TCP_PORT;
	private String expectPkg = "";
	private boolean autoAttachPkg = false;

	private DebugSettings() {
	}

	public void set(ADBDevice device, int ver, String pid, String name) {
		this.ver = ver;
		this.pid = pid;
		this.name = name;
		this.device = device;
		this.autoAttachPkg = false;
		this.expectPkg = "";
	}

	public DebugSettings setPid(String pid) {
		this.pid = pid;
		return this;
	}

	public DebugSettings setName(String name) {
		this.name = name;
		return this;
	}

	public String forwardJDWP() {
		int localPort = forwardTcpPort;
		String resultDesc = "";
		try {
			do {
				ADBDevice.ForwardResult rst = device.forwardJDWP(localPort + "", pid);
				if (rst.state == 0) {
					forwardTcpPort = localPort;
					return "";
				}
				if (rst.state == 1) {
					if (rst.desc.contains("Only one usage of each socket address")) { // port is taken by other process
						if (localPort < 65536) {
							localPort++; // retry
							continue;
						}
					}
				}
				resultDesc = rst.desc;
				break;
			} while (true);
		} catch (Exception e) {
			LOG.error("JDWP forward error", e);
		}
		if (StringUtils.isEmpty(resultDesc)) {
			resultDesc = NLS.str("adb_dialog.forward_fail");
		}
		return resultDesc;
	}

	// we have to remove all ports that forwarding the jdwp:pid, otherwise our JDWP handshake may fail.
	public void clearForward() {
		String jdwpPid = " jdwp:" + pid;
		String tcpPort = " tcp:" + forwardTcpPort;
		try {
			List<String> list = ADB.listForward(device.getDeviceInfo().getAdbHost(),
					device.getDeviceInfo().getAdbPort());
			for (String s : list) {
				if (s.startsWith(device.getSerial()) && s.endsWith(jdwpPid) && !s.contains(tcpPort)) {
					String[] fields = s.split("\\s+");
					for (String field : fields) {
						if (field.startsWith("tcp:")) {
							try {
								device.removeForward(field.substring("tcp:".length()));
							} catch (Exception e) {
								LOG.error("JDWP remove forward error", e);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			LOG.error("JDWP clear forward error", e);
		}
	}

	public boolean isBeingDebugged() {
		String jdwpPid = " jdwp:" + pid;
		String tcpPort = " tcp:" + forwardTcpPort;
		try {
			List<String> list = ADB.listForward(device.getDeviceInfo().getAdbHost(),
					device.getDeviceInfo().getAdbPort());
			for (String s : list) {
				if (s.startsWith(device.getSerial()) && s.endsWith(jdwpPid)) {
					return !s.contains(tcpPort);
				}
			}
		} catch (Exception e) {
			LOG.error("ADB list forward error", e);
		}
		return false;
	}

	public int getVer() {
		return ver;
	}

	public String getPid() {
		return pid;
	}

	public String getName() {
		return name;
	}

	public ADBDevice getDevice() {
		return device;
	}

	public int getForwardTcpPort() {
		return forwardTcpPort;
	}

	public String getExpectPkg() {
		return expectPkg;
	}

	public void setExpectPkg(String expectPkg) {
		this.expectPkg = expectPkg;
	}

	public boolean isAutoAttachPkg() {
		return autoAttachPkg;
	}

	public void setAutoAttachPkg(boolean autoAttachPkg) {
		this.autoAttachPkg = autoAttachPkg;
	}
}
