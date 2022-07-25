package jadx.gui.device.protocol;

import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.log.LogUtils;

public class ADBDeviceInfo {
	private static final Logger LOG = LoggerFactory.getLogger(ADBDeviceInfo.class);
	private final String adbHost;
	private final int adbPort;
	private final String serial;
	private final String state;
	private final String model;
	private final String allInfo;

	/**
	 * Store the device info property values like "device" "model" "product" or "transport_id"
	 */
	private final Map<String, String> propertiesMap = new TreeMap<>();

	ADBDeviceInfo(String info, String host, int port) {
		String[] infoFields = info.trim().split("\\s+");
		allInfo = String.join(" ", infoFields);
		if (infoFields.length > 2) {
			serial = infoFields[0];
			state = infoFields[1];

			for (int i = 2; i < infoFields.length; i++) {
				String field = infoFields[i];
				int idx = field.indexOf(':');
				if (idx > 0) {
					String key = field.substring(0, idx);
					String value = field.substring(idx + 1);
					if (!value.isEmpty()) {
						propertiesMap.put(key, value);
					}
				}
			}
			model = propertiesMap.getOrDefault("model", serial);
		} else {
			LOG.error("Unable to extract device information from {}", LogUtils.escape(info));
			serial = "";
			state = "unknown";
			model = "unknown";
		}
		adbHost = host;
		adbPort = port;
	}

	public boolean isOnline() {
		return state.equals("device");
	}

	public String getAdbHost() {
		return adbHost;
	}

	public int getAdbPort() {
		return adbPort;
	}

	public String getSerial() {
		return serial;
	}

	public String getState() {
		return state;
	}

	public String getModel() {
		return model;
	}

	public String getAllInfo() {
		return allInfo;
	}

	public String getProperty(String key) {
		return this.propertiesMap.get(key);
	}

	@Override
	public String toString() {
		return allInfo;
	}

}
