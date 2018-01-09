package jadx.core.xmlgen.entry;

public class EntryConfig {
	private String language;
	private String country;
	private String density;
	private String screenSize;
	private String sdkVersion;
	private String screenLayout;
	private String smallestScreenWidthDp;
	private String orientation;
	private String screenWidthDp;
	private String screenHeightDp;

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getLocale() {
		StringBuilder sb = new StringBuilder();
		if (screenSize != null) {
			if (sb.length() != 0) {
				sb.append("-");
			}
			sb.append(screenSize);
		} else if (screenHeightDp != null) {
			if (sb.length() != 0) {
				sb.append("-");
			}
			sb.append(screenHeightDp);
		} else if (screenWidthDp != null) {
			if (sb.length() != 0) {
				sb.append("-");
			}
			sb.append(screenWidthDp);
		} else if (screenLayout != null) {
			if (sb.length() != 0) {
				sb.append("-");
			}
			sb.append(screenLayout);
		} else if (smallestScreenWidthDp != null) {
			if (sb.length() != 0) {
				sb.append("-");
			}
			sb.append(smallestScreenWidthDp);
		} else if (density != null) {
			sb.append(density);
		}
		if (language != null) {
			if (sb.length() != 0) {
				sb.append("-");
			}
			sb.append(language);
		}
		if (country != null) {
			sb.append("-r").append(country);
		}
		if (orientation != null) {
			if (sb.length() != 0) {
				sb.append("-");
			}
			sb.append(orientation);
		}
		if (sdkVersion != null) {
			if (sb.length() != 0) {
				sb.append("-");
			}
			sb.append(sdkVersion);
		}
		return sb.toString();
	}

	public String getDensity() {
		return density;
	}

	public void setDensity(String density) {
		this.density = density;
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

	public void setScreenSize(String screenSize) {
		this.screenSize = screenSize;
	}

	public String getScreenSize() {
		return screenSize;
	}

	public void setSdkVersion(String sdkVersion) {
		this.sdkVersion = sdkVersion;
	}

	public String getSdkVersion() {
		return sdkVersion;
	}

	public void setScreenLayout(String screenLayout) {
		this.screenLayout = screenLayout;
	}

	public String getScreenLayout() {
		return screenLayout;
	}

	public void setSmallestScreenWidthDp(String smallestScreenWidthDp) {
		this.smallestScreenWidthDp = smallestScreenWidthDp;
	}

	public String getSmallestScreenWidthDp() {
		return smallestScreenWidthDp;
	}

	public void setOrientation(String orientation) {
		this.orientation = orientation;
	}

	public String getOrientation() {
		return orientation;
	}

	public void setScreenWidthDp(String screenWidthDp) {
		this.screenWidthDp = screenWidthDp;
	}

	public String getScreenWidthDp() {
		return screenWidthDp;
	}

	public void setScreenHeightDp(String screenHeightDp) {
		this.screenHeightDp = screenHeightDp;
	}

	public String getScreenHeightDp() {
		return screenHeightDp;
	}
}
