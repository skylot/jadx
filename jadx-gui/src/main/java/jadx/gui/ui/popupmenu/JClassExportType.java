package jadx.gui.ui.popupmenu;

public enum JClassExportType {
	Code("java"),
	Smali("smali"),
	Simple("java"),
	Fallback("java");

	final String extension;

	JClassExportType(String extension) {
		this.extension = extension;
	}
}
