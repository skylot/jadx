package jadx.core.xmlgen.entry;

public class EntryConfig {
	private String language;
	private String country;

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getLanguage() {
		return language;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getCountry() {
		return country;
	}

	public String getLocale() {
		StringBuilder sb = new StringBuilder();
		if (language != null) {
			sb.append(language);
		}
		if (country != null) {
			sb.append("-r").append(country);
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getLocale());
		if (sb.length() != 0) {
			sb.insert(0, " [");
			sb.append(']');
		}
		return sb.toString();
	}
}
