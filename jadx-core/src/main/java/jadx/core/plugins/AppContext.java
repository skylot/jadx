package jadx.core.plugins;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.gui.JadxGuiContext;
import jadx.core.plugins.files.IJadxFilesGetter;

public class AppContext {

	private @Nullable JadxGuiContext guiContext;

	private IJadxFilesGetter filesGetter;

	public @Nullable JadxGuiContext getGuiContext() {
		return guiContext;
	}

	public void setGuiContext(@Nullable JadxGuiContext guiContext) {
		this.guiContext = guiContext;
	}

	public IJadxFilesGetter getFilesGetter() {
		return Objects.requireNonNull(filesGetter);
	}

	public void setFilesGetter(IJadxFilesGetter filesGetter) {
		this.filesGetter = filesGetter;
	}
}
