package jadx.gui.cache.usage;

final class FldRef {
	private final String cls;
	private final String shortId;

	FldRef(String cls, String shortId) {
		this.cls = cls;
		this.shortId = shortId;
	}

	public String getCls() {
		return cls;
	}

	public String getShortId() {
		return shortId;
	}
}
