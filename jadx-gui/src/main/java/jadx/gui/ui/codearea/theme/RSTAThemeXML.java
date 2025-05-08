package jadx.gui.ui.codearea.theme;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RSTAThemeXML implements IEditorTheme {
	private static final Logger LOG = LoggerFactory.getLogger(RSTAThemeXML.class);

	private final Path themePath;
	private final String name;

	private Theme loadedTheme;

	public RSTAThemeXML(Path themeXmlPath, String name) {
		this.themePath = themeXmlPath;
		this.name = name;
	}

	@Override
	public String getId() {
		return "file:" + themePath;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void load() {
		try {
			try (InputStream is = Files.newInputStream(themePath)) {
				loadedTheme = Theme.load(is);
			}
		} catch (Exception e) {
			LOG.warn("Failed to load editor theme: {}", themePath, e);
			loadedTheme = new Theme(new RSyntaxTextArea());
		}
	}

	@Override
	public void apply(RSyntaxTextArea textArea) {
		loadedTheme.apply(textArea);
	}

	@Override
	public void unload() {
		loadedTheme = null;
	}
}
