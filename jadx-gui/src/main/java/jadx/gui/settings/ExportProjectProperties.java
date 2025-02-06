package jadx.gui.settings;

public class ExportProjectProperties {
	private boolean skipSources;
	private boolean skipResources;
	private boolean asGradleMode;
	private String exportPath;

	public boolean isSkipSources() {
		return skipSources;
	}

	public void setSkipSources(boolean skipSources) {
		this.skipSources = skipSources;
	}

	public boolean isSkipResources() {
		return skipResources;
	}

	public void setSkipResources(boolean skipResources) {
		this.skipResources = skipResources;
	}

	public boolean isAsGradleMode() {
		return asGradleMode;
	}

	public void setAsGradleMode(boolean asGradleMode) {
		this.asGradleMode = asGradleMode;
	}

	public String getExportPath() {
		return exportPath;
	}

	public void setExportPath(String exportPath) {
		this.exportPath = exportPath;
	}
}
