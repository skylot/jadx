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
	private final String application;

	public ApplicationParams(String applicationLabel, Integer minSdkVersion, Integer targetSdkVersion, Integer versionCode,
			String versionName, String mainActivtiy, String application) {
		this.applicationLabel = applicationLabel;
		this.minSdkVersion = minSdkVersion;
		this.targetSdkVersion = targetSdkVersion;
		this.versionCode = versionCode;
		this.versionName = versionName;
		this.mainActivtiy = mainActivtiy;
		this.application = application;
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

	public String getMainActivity() {
		return mainActivtiy;
	}

	public JavaClass getMainActivityJavaClass(JadxDecompiler decompiler) {
		return decompiler.searchJavaClassByOrigFullName(mainActivtiy);
	}

	public String getApplication() {
		return application;
	}

	public JavaClass getApplicationJavaClass(JadxDecompiler decompiler) {
		return decompiler.searchJavaClassByOrigFullName(application);
	}
}
