package jadx.core.export;

public class ApplicationParams {

	private final String applicationLabel;
	private final Integer minSdkVersion;
	private final Integer targetSdkVersion;

	public ApplicationParams(String applicationLabel, Integer minSdkVersion, Integer targetSdkVersion) {
		this.applicationLabel = applicationLabel;
		this.minSdkVersion = minSdkVersion;
		this.targetSdkVersion = targetSdkVersion;
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
}
