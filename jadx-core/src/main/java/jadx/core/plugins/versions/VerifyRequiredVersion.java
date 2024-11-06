package jadx.core.plugins.versions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import jadx.core.Jadx;

public class VerifyRequiredVersion {

	public static boolean isJadxCompatible(@Nullable String reqVersionStr) {
		return new VerifyRequiredVersion().isCompatible(reqVersionStr);
	}

	public static void verify(String requiredJadxVersion) {
		try {
			parse(requiredJadxVersion);
		} catch (Exception e) {
			throw new IllegalArgumentException("Malformed 'requiredJadxVersion': " + e.getMessage(), e);
		}
	}

	private final String jadxVersion;
	private final boolean unstable;

	private final boolean dev;

	public VerifyRequiredVersion() {
		this(Jadx.getVersion());
	}

	public VerifyRequiredVersion(String jadxVersion) {
		this.jadxVersion = jadxVersion;
		this.unstable = jadxVersion.startsWith("r");
		this.dev = jadxVersion.equals(Jadx.VERSION_DEV);
	}

	public boolean isCompatible(@Nullable String reqVersionStr) {
		if (reqVersionStr == null || reqVersionStr.isEmpty()) {
			return true;
		}
		RequiredVersionData reqVer = parse(reqVersionStr);
		if (dev) {
			// keep version str parsing for verification
			return true;
		}
		if (unstable) {
			return VersionComparator.checkAndCompare(jadxVersion, reqVer.getUnstableRev()) >= 0;
		}
		return VersionComparator.checkAndCompare(jadxVersion, reqVer.getReleaseVer()) >= 0;
	}

	public String getJadxVersion() {
		return jadxVersion;
	}

	private static final Pattern REQ_VER_FORMAT = Pattern.compile("(\\d+\\.\\d+\\.\\d+),\\s+(r\\d+)");

	private static RequiredVersionData parse(String reqVersionStr) {
		Matcher matcher = REQ_VER_FORMAT.matcher(reqVersionStr);
		if (!matcher.matches()) {
			throw new RuntimeException("Expect format: " + REQ_VER_FORMAT + ", got: " + reqVersionStr);
		}
		return new RequiredVersionData(matcher.group(1), matcher.group(2));
	}

	private static final class RequiredVersionData {
		private final String releaseVer;
		private final String unstableRev;

		private RequiredVersionData(String releaseVer, String unstableRev) {
			this.releaseVer = releaseVer;
			this.unstableRev = unstableRev;
		}

		public String getReleaseVer() {
			return releaseVer;
		}

		public String getUnstableRev() {
			return unstableRev;
		}
	}
}
