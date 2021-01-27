package jadx.gui.ui.codearea;

public final class EditorTheme {
	private static final String RSTA_THEME_PATH = "/org/fife/ui/rsyntaxtextarea/themes/";

	private static final EditorTheme[] ALL_THEMES =
			new EditorTheme[] {
					new EditorTheme("default"),
					new EditorTheme("eclipse"),
					new EditorTheme("idea"),
					new EditorTheme("vs"),
					new EditorTheme("dark"),
					new EditorTheme("monokai"),
					new EditorTheme("druid")
			};

	public static EditorTheme[] getAllThemes() {
		return ALL_THEMES;
	}

	public static EditorTheme getDefaultTheme() {
		return ALL_THEMES[0];
	}

	private final String name;
	private final String path;

	public EditorTheme(String name) {
		this(name, RSTA_THEME_PATH + name + ".xml");
	}

	public EditorTheme(String name, String path) {
		this.name = name;
		this.path = path;
	}

	public String getName() {
		return name;
	}

	public String getPath() {
		return path;
	}

	@Override
	public String toString() {
		return name;
	}
}
