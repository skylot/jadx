package jadx.gui.device.protocol;

public class ADBDeviceInfo {
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

	static ADBDeviceInfo make(String info, String host, int port) {
		ADBDeviceInfo deviceInfo = new ADBDeviceInfo();
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
