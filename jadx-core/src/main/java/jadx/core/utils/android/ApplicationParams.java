package jadx.core.utils.android;

import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;

public class ApplicationParams {

	private final String applicationLabel;
	private final Integer minSdkVersion;
	private final Integer targetSdkVersion;
	private final Integer versionCode;
	private final String versionName;
	private final String mainActivtiy;

	public ApplicationParams(String applicationLabel, Integer minSdkVersion, Integer targetSdkVersion, Integer versionCode,
			String versionName, String mainActivtiy) {
		this.applicationLabel = applicationLabel;
		this.minSdkVersion = minSdkVersion;
		this.targetSdkVersion = targetSdkVersion;
		this.versionCode = versionCode;
		this.versionName = versionName;
		this.mainActivtiy = mainActivtiy;
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

	public String getMainActivityName() {
		return mainActivtiy;
	}

	public JavaClass getMainActivity(JadxDecompiler decompiler) {
		return decompiler.searchJavaClassByOrigFullName(mainActivtiy);
	}
}
