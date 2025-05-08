package jadx.gui.ui.codearea.theme;

import java.io.InputStream;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RSTABundledTheme implements IEditorTheme {
	private static final Logger LOG = LoggerFactory.getLogger(RSTABundledTheme.class);

	private static final String RSTA_THEME_PATH = "/org/fife/ui/rsyntaxtextarea/themes/";

	private final String name;

	private Theme loadedTheme;

	public RSTABundledTheme(String name) {
		this.name = name;
	}

	@Override
	public String getId() {
		return "RSTA:" + name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void load() {
		String path = RSTA_THEME_PATH + name + ".xml";
		try {
			try (InputStream is = RSTABundledTheme.class.getResourceAsStream(path)) {
				loadedTheme = Theme.load(is);
			}
		} catch (Throwable t) {
			LOG.error("Failed to load editor theme: {}", path, t);
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
