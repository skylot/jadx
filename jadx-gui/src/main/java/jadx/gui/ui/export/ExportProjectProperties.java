package jadx.gui.ui.export;

import org.jetbrains.annotations.Nullable;

import jadx.core.export.ExportGradleType;

public class ExportProjectProperties {
	private boolean skipSources;
	private boolean skipResources;
	private boolean asGradleMode;
	private @Nullable ExportGradleType exportGradleType;
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

	public @Nullable ExportGradleType getExportGradleType() {
		return exportGradleType;
	}

	public void setExportGradleType(@Nullable ExportGradleType exportGradleType) {
		this.exportGradleType = exportGradleType;
	}

	public String getExportPath() {
		return exportPath;
	}

	public void setExportPath(String exportPath) {
		this.exportPath = exportPath;
	}

	@Override
	public String toString() {
		return "ExportProjectProperties{exportPath='" + exportPath + '\''
				+ ", asGradleMode=" + asGradleMode
				+ ", exportGradleType=" + exportGradleType
				+ ", skipSources=" + skipSources
				+ ", skipResources=" + skipResources
				+ '}';
	}
}
