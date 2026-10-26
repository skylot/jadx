package jadx.gui.plugins.apiscan;

public enum ApiCategory {
	PROCESS("Process execution"),
	FILES("File access"),
	NETWORK("Network access"),
	REFLECTION("Reflection"),
	NATIVE_CODE("Native code loading"),
	CLASS_LOADING("Class loading"),
	UNSAFE("Unsafe access"),
	EXIT("Application exit");

	private final String title;

	ApiCategory(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}
}
