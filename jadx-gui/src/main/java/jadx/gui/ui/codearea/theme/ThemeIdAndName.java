package jadx.gui.ui.codearea.theme;

public class ThemeIdAndName {
	private final String id;
	private final String name;

	public ThemeIdAndName(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	@Override
	public final boolean equals(Object other) {
		if (!(other instanceof ThemeIdAndName)) {
			return false;
		}
		return id.equals(((ThemeIdAndName) other).id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return name;
	}
}
