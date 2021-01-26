package jadx.core.export;

public class ApplicationParams {

	private final String applicationLabel;
	private final Integer minSdkVersion;
	private final Integer targetSdkVersion;
	private final Integer versionCode;
	private final String versionName;

	public ApplicationParams(String applicationLabel, Integer minSdkVersion, Integer targetSdkVersion, Integer versionCode,
			String versionName) {
		this.applicationLabel = applicationLabel;
		this.minSdkVersion = minSdkVersion;
		this.targetSdkVersion = targetSdkVersion;
		this.versionCode = versionCode;
		this.versionName = versionName;
	}

	public String getApplicationName() {
		return applicationLabel;
	}

	public Integer getMinSdkVersion() {
		return minSdkVersion;
	}

	public Integer getTargetSdkVersion() {
		return targetSdkVersion;
	}

	public Integer getVersionCode() {
		return versionCode;
	}

	public String getVersionName() {
		return versionName;
	}
}
