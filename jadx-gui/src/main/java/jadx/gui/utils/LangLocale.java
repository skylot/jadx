package jadx.gui.utils;

import java.util.Locale;

public class LangLocale {
	private Locale locale;

	// Don't remove. Used in json serialization
	public LangLocale() {
	}

	public LangLocale(Locale locale) {
		this.locale = locale;
	}

	public LangLocale(String l, String c) {
		this.locale = new Locale(l, c);
	}

	public Locale get() {
		return locale;
	}

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	@Override
	public String toString() {
		return NLS.str("language.name", this);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof LangLocale && locale.equals(((LangLocale) obj).get());
	}

	@Override
	public int hashCode() {
		return locale.hashCode();
	}
}
