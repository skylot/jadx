package jadx.core.clsp;

public enum ClspClassSource {
	APP(""),
	CORE("android.jar"),
	ANDROID_CAR("android.car.jar"),
	APACHE_HTTP_LEGACY_CLIENT("org.apache.http.legacy.jar");

	private final String jarFile;

	ClspClassSource(String jarFile) {
		this.jarFile = jarFile;
	}

	public String getJarFile() {
		return jarFile;
	}

	public static ClspClassSource getClspClassSource(String jarFile) {
		for (ClspClassSource classSource : ClspClassSource.values()) {
			if (classSource.getJarFile().equals(jarFile)) {
				return classSource;
			}
		}
		return APP;
	}
}
