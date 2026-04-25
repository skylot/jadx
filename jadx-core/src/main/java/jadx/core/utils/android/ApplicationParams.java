package jadx.core.utils.android;

import org.jetbrains.annotations.Nullable;

import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;

public class ApplicationParams {
	private @Nullable String applicationLabel;
	private @Nullable Integer minSdkVersion;
	private @Nullable Integer targetSdkVersion;
	private @Nullable Integer compileSdkVersion;
	private @Nullable Integer versionCode;
	private @Nullable String versionName;
	private @Nullable String mainActivity;
	private @Nullable String application;

	public @Nullable String getApplication() {
		return application;
	}

	public void setApplication(@Nullable String application) {
		this.application = application;
	}

	public @Nullable JavaClass getApplicationJavaClass(JadxDecompiler decompiler) {
		if (application == null) {
			return null;
		}
		return decompiler.searchJavaClassByAliasFullName(application);
	}

	public @Nullable String getApplicationLabel() {
		return applicationLabel;
	}

	public void setApplicationLabel(@Nullable String applicationLabel) {
		this.applicationLabel = applicationLabel;
	}

	public @Nullable String getMainActivity() {
		return mainActivity;
	}

	public void setMainActivity(@Nullable String mainActivity) {
		this.mainActivity = mainActivity;
	}

	public @Nullable JavaClass getMainActivityJavaClass(JadxDecompiler decompiler) {
		if (mainActivity == null) {
			return null;
		}
		return decompiler.searchJavaClassByAliasFullName(mainActivity);
	}

	public @Nullable Integer getCompileSdkVersion() {
		return compileSdkVersion;
	}

	public void setCompileSdkVersion(@Nullable Integer compileSdkVersion) {
		this.compileSdkVersion = compileSdkVersion;
	}

	public @Nullable Integer getMinSdkVersion() {
		return minSdkVersion;
	}

	public void setMinSdkVersion(@Nullable Integer minSdkVersion) {
		this.minSdkVersion = minSdkVersion;
	}

	public @Nullable Integer getTargetSdkVersion() {
		return targetSdkVersion;
	}

	public void setTargetSdkVersion(@Nullable Integer targetSdkVersion) {
		this.targetSdkVersion = targetSdkVersion;
	}

	public @Nullable Integer getVersionCode() {
		return versionCode;
	}

	public void setVersionCode(@Nullable Integer versionCode) {
		this.versionCode = versionCode;
	}

	public @Nullable String getVersionName() {
		return versionName;
	}

	public void setVersionName(@Nullable String versionName) {
		this.versionName = versionName;
	}
}
