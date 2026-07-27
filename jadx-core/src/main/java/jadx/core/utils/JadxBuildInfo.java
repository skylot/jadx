package jadx.core.utils;

import java.io.InputStream;
import java.util.Properties;

import jadx.core.Jadx;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class JadxBuildInfo {

	private static class BuildData {
		private final String jadxVersion;
		private final String jadxBundleType;

		private BuildData(String jadxVersion, String jadxBundleType) {
			this.jadxVersion = jadxVersion;
			this.jadxBundleType = jadxBundleType;
		}

		public String getJadxVersion() {
			return jadxVersion;
		}

		public String getJadxBundleType() {
			return jadxBundleType;
		}

		@Override
		public String toString() {
			return "{jadx-version:" + jadxVersion + ", jadx-bundle-type:" + jadxBundleType + "}";
		}
	}

	private static final BuildData BUILD_DATA = load();

	private static BuildData load() {
		try (InputStream in = JadxBuildInfo.class.getResourceAsStream("/jadx-build-info.properties")) {
			if (in == null) {
				throw new IllegalStateException("jadx-build-info.properties not found");
			}
			Properties props = new Properties();
			props.load(in);
			String version = props.getProperty("jadx-version", Jadx.VERSION_DEV);
			String bundleType = props.getProperty("jadx-bundle-type", "");
			return new BuildData(version, bundleType);
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to load build properties", e);
		}
	}

	public static String getJadxBundleType() {
		return BUILD_DATA.getJadxBundleType();
	}

	public static String getJadxVersion() {
		return BUILD_DATA.getJadxVersion();
	}
}
