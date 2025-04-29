package jadx.core.export;

public enum ExportGradleType {
	AUTO("Auto"),
	ANDROID_APP("Android App"),
	ANDROID_LIBRARY("Android Library"),
	SIMPLE_JAVA("Simple Java");

	private final String desc;

	ExportGradleType(String desc) {
		this.desc = desc;
	}

	public String getDesc() {
		return desc;
	}

	@Override
	public String toString() {
		return desc;
	}
}
