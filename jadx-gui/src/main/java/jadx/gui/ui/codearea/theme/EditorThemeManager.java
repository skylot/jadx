package jadx.gui.ui.codearea.theme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.StringUtils;
import jadx.gui.settings.JadxSettings;

public class EditorThemeManager {
	private static final Logger LOG = LoggerFactory.getLogger(EditorThemeManager.class);

	private final List<IEditorTheme> themes = new ArrayList<>();
	private final Map<String, IEditorTheme> themesMap = new HashMap<>();

	private IEditorTheme currentTheme = new FallbackEditorTheme();

	public EditorThemeManager(JadxSettings settings) {
		registerThemes();
		if (StringUtils.isEmpty(settings.getEditorTheme())) {
			// set default theme
			IEditorTheme defaultTheme = themes.get(0);
			settings.setEditorTheme(defaultTheme.getId());
		}
	}

	private void registerThemes() {
		registerTheme(new DynamicCodeAreaTheme());
		registerTheme(new RSTABundledTheme("default"));
		registerTheme(new RSTABundledTheme("eclipse"));
		registerTheme(new RSTABundledTheme("idea"));
		registerTheme(new RSTABundledTheme("vs"));
		registerTheme(new RSTABundledTheme("dark"));
		registerTheme(new RSTABundledTheme("monokai"));
		registerTheme(new RSTABundledTheme("druid"));
	}

	public void registerTheme(IEditorTheme editorTheme) {
		IEditorTheme prev = themesMap.put(editorTheme.getId(), editorTheme);
		if (prev != null) {
			themes.remove(prev);
		}
		themes.add(editorTheme);
	}

	public synchronized void setTheme(String id) {
		if (currentTheme.getId().equals(id)) {
			// already set
			return;
		}
		// resolve new
		IEditorTheme newTheme = themesMap.get(id);
		if (newTheme == null) {
			LOG.warn("Failed to resolve editor theme: {}", id);
			return;
		}
		// unload current
		unload();

		// load new
		try {
			newTheme.load();
		} catch (Throwable t) {
			LOG.warn("Failed to load editor theme: {}", id, t);
		}
		currentTheme = newTheme;
	}

	public void apply(RSyntaxTextArea textArea) {
		this.currentTheme.apply(textArea);
	}

	public ThemeIdAndName[] getThemeIdNameArray() {
		return themes.stream()
				.map(EditorThemeManager::toThemeIdAndName)
				.toArray(ThemeIdAndName[]::new);
	}

	public ThemeIdAndName getCurrentThemeIdName() {
		return toThemeIdAndName(currentTheme);
	}

	private static ThemeIdAndName toThemeIdAndName(IEditorTheme t) {
		return new ThemeIdAndName(t.getId(), t.getName());
	}

	public void unload() {
		try {
			currentTheme.unload();
		} catch (Throwable t) {
			LOG.warn("Failed to unload editor theme: {}", currentTheme.getId(), t);
		}
	}
}
